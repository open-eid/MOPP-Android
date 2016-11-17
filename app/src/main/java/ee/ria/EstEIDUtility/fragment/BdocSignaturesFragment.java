package ee.ria.EstEIDUtility.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;

import java.util.ArrayList;
import java.util.List;

import ee.ria.EstEIDUtility.activity.BrowseContainersActivity;
import ee.ria.EstEIDUtility.adapter.SignatureAdapter;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.Signature;
import ee.ria.libdigidocpp.Signatures;

public class BdocSignaturesFragment extends ListFragment {

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Intent intent = getActivity().getIntent();
        String bdocFileName = intent.getExtras().getString(BrowseContainersActivity.BDOC_NAME);

        Container container = Container.open(getActivity().getFilesDir().getAbsolutePath() + "/" + bdocFileName);

        List<Signature> signatures = createSignatures(container.signatures());

        SignatureAdapter signatureAdapter = new SignatureAdapter(getActivity(), signatures, bdocFileName);
        setListAdapter(signatureAdapter);

        registerForContextMenu(getListView());
    }

    private List<Signature> createSignatures(Signatures signatures) {
        List<Signature> signatureItems = new ArrayList<>();
        for (int i = 0; i < signatures.size(); i++) {
            signatureItems.add(signatures.get(i));
        }
        return signatureItems;
    }

}
