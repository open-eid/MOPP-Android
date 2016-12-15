package ee.ria.EstEIDUtility.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.view.View;

import java.io.File;
import java.util.List;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.adapter.SignatureAdapter;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.ContainerUtils;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.EstEIDUtility.util.LayoutUtils;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.Signature;

public class ContainerSignaturesFragment extends ListFragment {

    public static final String TAG = "CONT_SIGNATURES_FRAGMENT";
    private SignatureAdapter signatureAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String containerFileName = getArguments().getString(Constants.CONTAINER_NAME_KEY);
        String containerPath = getArguments().getString(Constants.CONTAINER_WORKING_PATH_KEY);

        Container container = FileUtils.getContainer(getContext().getFilesDir(), containerFileName);
        List<Signature> signatures = ContainerUtils.extractSignatures(container);

        signatureAdapter = new SignatureAdapter(getActivity(), signatures, FileUtils.getFile(containerPath, containerFileName), ContainerSignaturesFragment.this);
        setListAdapter(signatureAdapter);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        calculateFragmentHeight();
        setEmptyText(getText(R.string.empty_container_signatures));
    }

    public void calculateFragmentHeight() {
        LayoutUtils.calculateFragmentHeight(getListView());
    }

    public void addSignature(Signature signature) {
        signatureAdapter.add(signature);
        calculateFragmentHeight();
    }

}
