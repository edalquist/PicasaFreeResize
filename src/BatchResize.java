import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.ImmutableSet;
import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.media.MediaFileSource;
import com.google.gdata.data.media.MediaSource;
import com.google.gdata.data.media.mediarss.MediaContent;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;

/**
 * An effective but gross tool that will resize all of the images in a picasa account
 * down to the 2048 free storage size. Images are updated in place and retain all
 * dates/comments/attributes/exif data.
 * <br/>
 * <br/>
 * WARNING: This tool worked for me but you really should monitor your account and make sure it is doing what you expect! 
 * 
 * @author Eric Dalquist
 */
public class BatchResize {
    private static final Set<String> IMAGE_TYPES = ImmutableSet.of("image/jpeg", "image/png");
    private static final Pattern BAD_FILENAME_CHARS = Pattern.compile(":|/|\\\\");
    private static String convertPath;

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException("Expected usage:\n\t\tjava -jar BatchResize.jar /path/to/convert username@gmail.com password");
        }

        //Parse CLI arguments
        convertPath = args[0];
        final String username = args[1];
        final String password = args[2];
        final int availableProcessors;
        if (args.length >= 4) {
            availableProcessors = Integer.parseInt(args[3]);
        }
        else {
            availableProcessors = Runtime.getRuntime().availableProcessors();
        }

        //Setup the Picasa service API
        final PicasawebService myService = new PicasawebService("batch-resize-app");
        myService.setUserCredentials(username, password);

        //Get the album feed
        final URL feedUrl = new URL("https://picasaweb.google.com/data/feed/api/user/default?kind=album");
        final UserFeed myUserFeed;
        synchronized (myService) {
            myUserFeed = myService.getFeed(feedUrl, UserFeed.class);
        }

        //Setup the resize thread pool and tracking collections
        final ExecutorService resizePool = new ThreadPoolExecutor(availableProcessors, availableProcessors, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(availableProcessors * 4), new ThreadPoolExecutor.CallerRunsPolicy());
        final Queue<Future<ResizeRequest>> futures = new LinkedList<Future<ResizeRequest>>();
        final Set<String> ignoredTypes = new HashSet<String>();
        
        //Iterate over each album
        for (final AlbumEntry albumEntry : myUserFeed.getAlbumEntries()) {
            System.out.println(albumEntry.getId() + " - " + albumEntry.getTitle().getPlainText());

            //Need a custom album feed url to add imgmax=d to get original source images
            final URL albumFeedUrl = new URL("https://picasaweb.google.com/data/feed/api/user/default/albumid/"
                    + albumEntry.getGphotoId() + "?imgmax=d");

            //Iterate over each photo in the album
            final AlbumFeed albumFeed = myService.getFeed(albumFeedUrl, AlbumFeed.class);
            for (final PhotoEntry photoEntry : albumFeed.getPhotoEntries()) {
                checkResizeFutures(futures, false);
                System.out.print("\t" + photoEntry.getId() + " - " + photoEntry.getTitle().getPlainText());

                //Look through the media content for each entry, checking the content type and dimensions
                final List<MediaContent> mediaContents = photoEntry.getMediaContents();
                for (MediaContent mediaContent : mediaContents) {
                    final String type = mediaContent.getType();
                    if (IMAGE_TYPES.contains(type)) {
                        final int width = mediaContent.getWidth();
                        final int height = mediaContent.getHeight();

                        if (width > 2048 || height > 2048) {
                            final String url = mediaContent.getUrl();
                            System.out.println(" - Resize - " + width + "x" + height + " - " + url);

                            final File imageFile = new File(new File("."), BAD_FILENAME_CHARS.matcher(photoEntry.getId()).replaceAll("_") + "." + type.substring(type.lastIndexOf('/') + 1));;
                            final File resizedImageFile = new File(imageFile.getParentFile(), "BATCH_RESIZED_" + imageFile.getName());
                            final ResizeRequest resizeRequest = new ResizeRequest(photoEntry, type, url, imageFile, resizedImageFile);
                            
                            //Download the image and submit it to the resizing thread pool
                            final Future<ResizeRequest> future = download(myService, resizeRequest, resizePool);
                            futures.add(future);
                        }
                        else {
                            System.out.println(" -  Skip - " + width + "x" + height);
                        }
                    }
                    else if (ignoredTypes.add(type)) {
                        System.out.println(" - Ignore - " + type);
                        break;
                    }
                }
            }
        }
        
        resizePool.shutdown();
        
        checkResizeFutures(futures, true);
    }
    
    /**
     * Check each resize future, if complete upload the result.
     * @param wait If true {@link Future#get()} is called on each {@link Future}.
     *             If false it is only called on futures where {@link Future#isDone()} returns true, also the method returns after the first future that returns true is uploaded.  
     */
    private static void checkResizeFutures(Collection<Future<ResizeRequest>> futures, boolean wait) throws InterruptedException, ExecutionException, IOException, ServiceException {
        final List<Future<?>> newFutures = new LinkedList<Future<?>>();
        newFutures.clear();
        
        for (final Iterator<Future<ResizeRequest>> iterator = futures.iterator(); iterator.hasNext();) {
            final Future<ResizeRequest> future = iterator.next();
            
            if (future.isDone() || wait) {
                final ResizeRequest resizeRequest = future.get();
                upload(resizeRequest);
                iterator.remove();
                break;
            }
        }
    }
    
    /**
     * Download the specified image and then submit it to the resize thread pool, returning the resize future.
     */
    private static Future<ResizeRequest> download(PicasawebService myService, ResizeRequest resizeRequest, ExecutorService resizePool) throws IOException, ServiceException {
        System.out.println("\t\tDownloading " + resizeRequest.photoEntry.getTitle().getPlainText());
        final com.google.gdata.data.MediaContent mc = new com.google.gdata.data.MediaContent();
        mc.setUri(resizeRequest.photoUrl);
        final MediaSource media = myService.getMedia(mc);
        final InputStream input = media.getInputStream();
        try {
            final BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(resizeRequest.originalFile));
            try {
                IOUtils.copy(input, output);
            }
            finally {
                IOUtils.closeQuietly(output);
            }
        }
        finally {
            IOUtils.closeQuietly(input);
        }
        
        System.out.println("\t\tDownloaded " + resizeRequest.photoEntry.getTitle().getPlainText());
        return resizePool.submit(new ResizeWorker(resizeRequest));
    }
    
    private static void upload(ResizeRequest resizeRequest) throws IOException, ServiceException {
        System.out.println("\t\tUploading " + resizeRequest.photoEntry.getTitle().getPlainText());
        final MediaFileSource mediaFileSource = new MediaFileSource(resizeRequest.resizedFile, resizeRequest.type);
        resizeRequest.photoEntry.setMediaSource(mediaFileSource);
        resizeRequest.photoEntry.updateMedia(true);
        
        //Remove the resized file
        resizeRequest.resizedFile.delete();
        
        System.out.println("\t\tUploaded " + resizeRequest.photoEntry.getTitle().getPlainText());
    }
    
    private static final class ResizeRequest {
        private final PhotoEntry photoEntry;
        private final String type;
        private final String photoUrl;
        private final File originalFile;
        private final File resizedFile;

        public ResizeRequest(PhotoEntry photoEntry, String type, String photoUrl, File originalFile, File resizedFile) {
            this.photoEntry = photoEntry;
            this.type = type;
            this.photoUrl = photoUrl;
            this.originalFile = originalFile;
            this.resizedFile = resizedFile;
        }
    }
    
    private static final class ResizeWorker implements Callable<ResizeRequest> {
        private final ResizeRequest resizeRequest;

        public ResizeWorker(ResizeRequest resizeRequest) {
            this.resizeRequest = resizeRequest;
        }

        @Override
        public ResizeRequest call() throws Exception {
            System.out.println("\t\tResizing: " + resizeRequest.photoEntry.getTitle().getPlainText());
            
            final ProcessBuilder pb = new ProcessBuilder(
                    convertPath, 
                    resizeRequest.originalFile.getCanonicalPath(), 
                    "-resize", "2048x2048>", 
                    resizeRequest.resizedFile.getCanonicalPath());

            final Process p = pb.start();
            if (p.waitFor() != 0) {
                final String out = IOUtils.toString(p.getInputStream());
                final String err = IOUtils.toString(p.getErrorStream());
                throw new IllegalStateException("Failed to resize image: " + resizeRequest.originalFile + "\n\n" + out + "\n\n" + err);
                
            }

            //Remove original file
            resizeRequest.originalFile.delete();
            
            System.out.println("\t\tResized: " + resizeRequest.photoEntry.getTitle().getPlainText());
            
            return resizeRequest;
        }
    }
}
