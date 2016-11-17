package ee.ria.EstEIDUtility.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ee.ria.EstEIDUtility.activity.BrowseContainersActivity;
import ee.ria.EstEIDUtility.R;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFile;

/**
 * A simple {@link Fragment} subclass.
 */
public class FileContentFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragLayout = inflater.inflate(R.layout.fragment_file_content, container, false);

        TextView docContent = (TextView) fragLayout.findViewById(R.id.docContent);

        Intent intent = getActivity().getIntent();

        String fileName = intent.getExtras().getString(BrowseContainersActivity.FILE_NAME);
        String bdocName = intent.getExtras().getString(BrowseContainersActivity.BDOC_NAME);

        Container bdocContainer = Container.open(getActivity().getFilesDir().getAbsolutePath() + "/" + bdocName);

        for (int i = 0; i < bdocContainer.dataFiles().size(); i++) {
            DataFile dataFile = bdocContainer.dataFiles().get(i);
            if (dataFile.fileName().equals(fileName)) {
                //TODO: file content?
            }
        }

        /*File file = getActivity().getFileStreamPath(fileName);
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "onCreateView: ", e);
        } catch (IOException e) {
            Log.e(TAG, "onCreateView: ", e);
        }*/

        docContent.setText("Hello world!");

        return fragLayout;
    }

}
