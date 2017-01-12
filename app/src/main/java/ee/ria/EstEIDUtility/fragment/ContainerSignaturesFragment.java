package ee.ria.EstEIDUtility.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.view.View;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.adapter.SignatureAdapter;
import ee.ria.EstEIDUtility.container.ContainerBuilder;
import ee.ria.EstEIDUtility.container.ContainerFacade;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.LayoutUtils;
import ee.ria.libdigidocpp.Signature;

public class ContainerSignaturesFragment extends ListFragment {

    public static final String TAG = ContainerSignaturesFragment.class.getName();
    private SignatureAdapter signatureAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String containerPath = getArguments().getString(Constants.CONTAINER_PATH_KEY);
        ContainerFacade containerFacade = ContainerBuilder.aContainer(getContext()).fromExistingContainer(containerPath).build();;
        signatureAdapter = new SignatureAdapter(getActivity(), containerFacade, this);
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
