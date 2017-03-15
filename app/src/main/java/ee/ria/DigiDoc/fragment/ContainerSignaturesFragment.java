/*
 * Copyright 2017 Riigi Infosüsteemide Amet
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package ee.ria.DigiDoc.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.view.View;

import java.io.File;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.adapter.SignatureAdapter;
import ee.ria.DigiDoc.container.ContainerBuilder;
import ee.ria.DigiDoc.container.ContainerFacade;
import ee.ria.DigiDoc.container.SignatureFacade;
import ee.ria.DigiDoc.util.Constants;
import ee.ria.DigiDoc.util.LayoutUtils;

public class ContainerSignaturesFragment extends ListFragment {

    public static final String TAG = ContainerSignaturesFragment.class.getName();
    private SignatureAdapter signatureAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String containerPath = getArguments().getString(Constants.CONTAINER_PATH_KEY);
        ContainerFacade containerFacade = ContainerBuilder.aContainer(getContext()).fromExistingContainer(containerPath).build();
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

    public void addSignature(SignatureFacade signatureFacade) {
        if (signatureFacade != null) {
            signatureAdapter.add(signatureFacade);
            calculateFragmentHeight();
        }
    }

    public void updateContainer(File containerFile) {
        signatureAdapter.updateContainerFile(containerFile);
    }

}
