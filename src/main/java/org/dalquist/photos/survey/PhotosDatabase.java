package org.dalquist.photos.survey;

import javax.annotation.PreDestroy;

import org.dalquist.photos.survey.firebase.BlockingAuthResultHandler;
import org.dalquist.photos.survey.firebase.OverallCompletionListener;
import org.dalquist.photos.survey.model.Album;
import org.dalquist.photos.survey.model.Config;
import org.dalquist.photos.survey.model.Credentials;
import org.dalquist.photos.survey.model.Media;
import org.dalquist.photos.survey.model.SourceId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.firebase.client.Firebase;
import com.google.common.collect.ImmutableMap;

@Service
public final class PhotosDatabase {
  private final OverallCompletionListener overallCompletionListener =
      new OverallCompletionListener();
  private final Firebase firebase;

  @Autowired
  public PhotosDatabase(Config config) {
    firebase = new Firebase("https://photosdb.firebaseio.com/");
    Credentials firebaseCredentials = config.getFirebaseCredentials();

    // Request auth and wait for it to complete
    BlockingAuthResultHandler waitingAuthResultHandler = new BlockingAuthResultHandler();
    firebase.authWithPassword(firebaseCredentials.getEmail(), firebaseCredentials.getPassword(),
        waitingAuthResultHandler);
    waitingAuthResultHandler.waitForAuth();
  }

  @PreDestroy
  public void stop() {
    overallCompletionListener.waitForCompletion();
  }

  public void writeAlbum(SourceId sourceId, Album album) {
    firebase
        .child("sources")
        .child(sourceId.getId())
        .child("albums")
        .child(album.getId())
        .setValue(ImmutableMap.of("name", album.getName()),
            overallCompletionListener.getCompletionListener());
  }

  public void add(Media entry) {
//    media.setValue(entry, new Firebase.CompletionListener() {
//      @Override
//      public void onComplete(FirebaseError arg0, Firebase arg1) {
//        System.out.println("DONE:" + arg0);
//      }
//    });
  }


  // private final JacksonUtils jacksonUtils;
  //
  // private final SortedMap<MediaId, Media> media = new TreeMap<>();
  //
  // public PhotosDatabase(String filename) throws IOException {
  // dbFile = new File(filename);
  // load();
  // }
  //
  // public void add(Media entry) {
  // // Perform replacement of existing entries
  // media.put(entry.getMediaId(), entry);
  // }
  //
  // public void load() throws IOException {
  // if (!dbFile.exists()) {
  // return;
  // }
  // // TODO
  // // try (Reader in = new FileReader(dbFile)) {
  // // ObjectMapper objectMapper = JacksonUtils.getObjectMapper();
  // //
  // // MappingIterator<Media> mediaEntryItr =
  // // objectMapper.reader(Media.class).readValues(in);
  // //
  // // List<Media> photosBuilder = new LinkedList<>();
  // // Iterators.addAll(photosBuilder, mediaEntryItr);
  // // photos = new TreeSet<>(photosBuilder);
  // //
  // // int dupeCount = photosBuilder.size() - photos.size();
  // // if (dupeCount > 0) {
  // // throw new IllegalStateException("The photosDb file contains duplicate " + dupeCount +
  // "entries!");
  // // }
  // //
  // // System.out.println("Loaded " + photos.size() + " photos");
  // // }
  // }
  //
  // public void save() throws IOException {
  // Database db = new Database();
  // db.media = media.values();
  //
  // File tempFile = File.createTempFile("pdb_", "_tmp.json");
  // tempFile.deleteOnExit();
  // try {
  // try (Writer out = new FileWriter(tempFile)) {
  // OBJECT_MAPPER.writer().writeValue(out, db);
  // }
  // Files.move(tempFile, dbFile);
  // } finally {
  // tempFile.delete();
  // }
  // }
  //
  // static class Database {
  // private Collection<Media> media;
  //
  // public Collection<Media> getMedia() {
  // return media;
  // }
  //
  // public void setMedia(Collection<Media> media) {
  // this.media = media;
  // }
  // }
}
