package cs213.PhotosApp19;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import java.io.FileDescriptor;
import java.util.ArrayList;

import cs213.PhotosApp19.adapter.PhotoAdapter;
import cs213.PhotosApp19.model.Album;
import cs213.PhotosApp19.model.Photo;

/**
 * AlbumInterface handles all functionalities of albums.
 * @author Magdi Aref
 * @author Muskan Burman
 */
public class AlbumInterface extends AppCompatActivity {
    private ArrayList<Album> albums;
    private Album checkedAlbum;
    private ListView listView;
    private String path;
    private int albumPosition = 0;

    /**
     * onCreate initiates the photo view on reading the photo.
     * @param savedInstanceState as the saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        path = this.getApplicationInfo().dataDir + "/data.dat";
        Intent intent = getIntent();
        albums = (ArrayList<Album>) intent.getSerializableExtra("albums");
        albumPosition = intent.getIntExtra("albumPosition", 0);
        checkedAlbum = albums.get(albumPosition);

        PhotoAdapter adapter = new PhotoAdapter(this, R.layout.photo_view, checkedAlbum.getPhotos());
        adapter.setNotifyOnChange(true);
        listView = findViewById(R.id.listView);
        listView.setAdapter(adapter);
        listView.setItemChecked(0, true);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                listView.setItemChecked(position, true);
            }
        });
    }

    /**
     * Opens the selected photo.
     * @param view  as view
     */
    public void openPhoto(View view) {
        if (listView.getAdapter().getCount() == 0)
            return;

        Intent intent = new Intent(this, PhotoInterface.class);

        intent.putExtra("albums", albums);
        intent.putExtra("albumPosition", albumPosition);
        intent.putExtra("photoPosition", listView.getCheckedItemPosition());
        startActivity(intent);
    }

    /**
     * addPhoto calls Open document and adds the desired new photo.
     * @param view as view
     */
    public void addPhoto(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        startActivityForResult(intent, 42);
    }

    /**
     * Adds the photo to the album.
     * @param requestCode as Request code in Int
     * @param resultCode result code as int
     * @param resultData as the Intent
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        if (requestCode == 42 && resultCode == Activity.RESULT_OK) {

            if (resultData != null) {
                Uri uri = resultData.getData();
                Bitmap bitmap = null;
                try {
                    ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
                    FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                    bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                    parcelFileDescriptor.close();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }

                String caption = uri.getLastPathSegment();
                Photo photo = new Photo(caption, bitmap);
                PhotoAdapter adapter = (PhotoAdapter) listView.getAdapter();

                for (int index = 0; index < adapter.getCount(); index++)
                    if (photo.equals(adapter.getItem(index))) {
                        new AlertDialog.Builder(this)
                                .setMessage("A photo with the caption \"" + caption + "\" already exists in this checkedAlbum.")
                                .setPositiveButton("OK", null)
                                .show();

                        return;
                    }

                adapter.add(photo);
                DataSaver.saveData(albums, path);
            }
        }
    }

    /**
     * Removes the selected photo from the album.
     * @param view as view
     */
    public void removePhoto(View view) {
        final PhotoAdapter adapter = (PhotoAdapter) listView.getAdapter();

        if (adapter.getCount() == 0) {
            new AlertDialog.Builder(this)
                    .setMessage("This checkedAlbum does not have any photos.")
                    .setPositiveButton("OK", null)
                    .show();

            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final int checkedItemPosition = listView.getCheckedItemPosition();
        final Photo checkedPhoto = adapter.getItem(checkedItemPosition);

        builder.setMessage("Are you sure you want to remove \"" + checkedPhoto.getCaption() + "\"?");
        builder.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        adapter.remove(checkedPhoto);
                        DataSaver.saveData(albums, path);
                        listView.setItemChecked(checkedItemPosition, true);
                    }
                });

        builder.setNegativeButton("No",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        builder.show();
    }

    /**
     * Copies the photo from one album into another.
     * @param view as view
     */
    public void copyPhoto(View view) {
        ArrayList<String> albumNames = new ArrayList<String>();

        for (Album currentAlbum : albums) {
            if (!currentAlbum.getName().equals(checkedAlbum.getName()))
                albumNames.add(currentAlbum.getName());
        }

        final CharSequence[] albumNamesArray = albumNames.toArray(new CharSequence[albumNames.size()]);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final PhotoAdapter adapter = (PhotoAdapter) listView.getAdapter();
        final Album destination = new Album(albumNamesArray[0].toString());

        builder.setSingleChoiceItems(albumNamesArray, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                destination.setName(albumNamesArray[which].toString());
            }
        })
                .setPositiveButton("Copy", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Photo photo = adapter.getItem(listView.getCheckedItemPosition());

                        for (Album currentAlbum : albums) {
                            if (currentAlbum.getName().equals(destination.getName())) {
                                for (Photo currentPhoto : currentAlbum.getPhotos()) {
                                    if (currentPhoto.equals(photo)) {
                                        new AlertDialog.Builder(builder.getContext())
                                                .setMessage("A photo with the caption \"" + photo.getCaption() + "\" already exists in \"" + currentAlbum.getName() + "\".")
                                                .setPositiveButton("OK", null)
                                                .show();

                                        return;
                                    }

                                }
                                currentAlbum.getPhotos().add(photo);
                                DataSaver.saveData(albums, path);
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        builder.show();
    }

    /**
     * Moves the photo from album into another.
     * @param view takes the view.
     */
    public void movePhoto(View view) {
        ArrayList<String> albumNames = new ArrayList<String>();

        for (Album currentAlbum : albums) {
            if (!currentAlbum.getName().equals(checkedAlbum.getName()))
                albumNames.add(currentAlbum.getName());
        }

        final CharSequence[] albumNamesArray = albumNames.toArray(new CharSequence[albumNames.size()]);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final PhotoAdapter adapter = (PhotoAdapter) listView.getAdapter();
        final Album destination = new Album(albumNamesArray[0].toString());

        builder.setSingleChoiceItems(albumNamesArray, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                destination.setName(albumNamesArray[which].toString());
            }
        })
                .setPositiveButton("Move", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Photo photo = adapter.getItem(listView.getCheckedItemPosition());
                        adapter.remove(photo);

                        for (Album currentAlbum : albums) {
                            if (currentAlbum.getName().equals(destination.getName())) {
                                for (Photo currentPhoto : currentAlbum.getPhotos()) {
                                    if (currentPhoto.equals(photo)) {
                                        new AlertDialog.Builder(builder.getContext())
                                                .setMessage("A photo with the caption \"" + photo.getCaption() + "\" already exists in \"" + currentAlbum.getName() + "\".")
                                                .setPositiveButton("OK", null)
                                                .show();

                                        return;
                                    }

                                }
                                currentAlbum.getPhotos().add(photo);
                                DataSaver.saveData(albums, path);
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        builder.show();
    }

    /**
     * Lets the user change the caption of a photo after checking for repeats.
     * @param view
     */
    public void changeCaption(View view) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final PhotoAdapter adapter = (PhotoAdapter) listView.getAdapter();
        final EditText input = new EditText(this);

        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(adapter.getItem(listView.getCheckedItemPosition()).getCaption());
        input.setSelection(input.getText().length());
        builder.setView(input);

        builder.setPositiveButton("Recaption", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String photoCaption = input.getText().toString();

                for (int index = 0; index < adapter.getCount(); index++)
                    if (photoCaption.equals(adapter.getItem(index).getCaption())) {
                        new AlertDialog.Builder(builder.getContext())
                                .setMessage("An photo with the caption \"" + photoCaption + "\" already exists in this album.")
                                .setPositiveButton("OK", null)
                                .show();

                        return;
                    }
                adapter.getItem(listView.getCheckedItemPosition()).setCaption(photoCaption);
                adapter.notifyDataSetChanged();
                DataSaver.saveData(albums, path);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    /**
     * Handles the Home button functionalities.
     * @param item takes the Menu Item
     * @return boolean value
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent intent = new Intent(this, MainInterface.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
