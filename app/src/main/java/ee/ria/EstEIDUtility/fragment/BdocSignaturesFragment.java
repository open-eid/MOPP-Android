package ee.ria.EstEIDUtility.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.List;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.adapter.SignatureAdapter;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.ContainerUtils;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.Signature;

public class BdocSignaturesFragment extends ListFragment {

    public static final String TAG = "BDOC_DETAIL_SIGNATURES_FRAGMENT";
    private SignatureAdapter signatureAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String bdocFileName = getArguments().getString(Constants.BDOC_NAME);

        Container container = FileUtils.getContainer(getContext().getFilesDir(), bdocFileName);
        List<Signature> signatures = ContainerUtils.extractSignatures(container);

        signatureAdapter = new SignatureAdapter(getActivity(), signatures, bdocFileName);
        setListAdapter(signatureAdapter);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setFragmentHeight();
    }

    private void setFragmentHeight() {
        ListView listView = getListView();
        int totalHeight = 0;

        for (int i = 0; i < signatureAdapter.getCount(); i++) {
            View mView = signatureAdapter.getView(i, null, listView);
            mView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            totalHeight += mView.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (signatureAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();

        String emptyText = getResources().getString(R.string.empty_container_signatures);
        setEmptyText(emptyText);
    }

    public void addSignature(Signature signature) {
        signatureAdapter.add(signature);
        setFragmentHeight();
    }
}
