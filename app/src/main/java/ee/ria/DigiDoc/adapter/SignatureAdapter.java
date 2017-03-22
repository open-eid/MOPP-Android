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

package ee.ria.DigiDoc.adapter;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.x500.style.BCStyle;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.certificate.X509Cert;
import ee.ria.DigiDoc.container.ContainerBuilder;
import ee.ria.DigiDoc.container.ContainerFacade;
import ee.ria.DigiDoc.container.SignatureFacade;
import ee.ria.DigiDoc.fragment.ContainerSignaturesFragment;
import ee.ria.DigiDoc.util.DateUtils;
import ee.ria.DigiDoc.util.NotificationUtil;

public class SignatureAdapter extends ArrayAdapter<SignatureFacade> implements Filterable {

    private ContainerFacade containerFacade;
    private ContainerSignaturesFragment containerSignaturesFragment;

    private NotificationUtil notificationUtil;
    private Activity activity;

    static class ViewHolder {
        @BindView(R.id.personName) TextView name;
        @BindView(R.id.fileSize) TextView signed;
        @BindView(R.id.removeSignature) ImageView removeSignature;
        @BindView(R.id.isSigned) TextView isSigned;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    public SignatureAdapter(Activity activity, ContainerFacade containerFacade, ContainerSignaturesFragment containerSignaturesFragment) {
        super(activity, 0, containerFacade.getSignatures());
        this.activity = activity;
        this.containerFacade = containerFacade;
        this.containerSignaturesFragment = containerSignaturesFragment;
    }

    public void updateContainerFile(File containerFile) {
        containerFacade = ContainerBuilder.aContainer(getContext()).fromExistingContainer(containerFile).build();
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        notificationUtil = new NotificationUtil(activity);
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_container_signatures, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        SignatureFacade signatureFacade = getItem(position);

        if (signatureFacade != null) {
            String personInfo;
            byte[] der = signatureFacade.getSigningCertificateDer();
            if (der != null && der.length > 0) {
                X509Cert x509Cert = new X509Cert(der);

                String surname = x509Cert.getValueByObjectIdentifier(ASN1ObjectIdentifier.getInstance(BCStyle.SURNAME));
                String name = x509Cert.getValueByObjectIdentifier(ASN1ObjectIdentifier.getInstance(BCStyle.GIVENNAME));
                String serialNumber = x509Cert.getValueByObjectIdentifier(ASN1ObjectIdentifier.getInstance(BCStyle.SERIALNUMBER));

                if ((surname != null && !surname.isEmpty()) && (name != null && !name.isEmpty())) {
                    if (serialNumber != null) {
                        personInfo = String.format("%s %s (%s)", name, surname, serialNumber);
                    } else {
                        personInfo = String.format("%s %s", name, surname);
                    }
                } else {
                    String commonName = x509Cert.getValueByObjectIdentifier(ASN1ObjectIdentifier.getInstance(BCStyle.CN));

                    if (serialNumber != null) {
                        personInfo = String.format("%s (%s)", commonName, serialNumber);
                    } else {
                        personInfo = String.format("%s", commonName);
                    }
                }
            } else {
                personInfo = signatureFacade.getSignedBy();
            }

            viewHolder.name.setText(personInfo);
            viewHolder.signed.setText(DateUtils.formatSignedDate(signatureFacade.getTrustedSigningTime()));

            if (signatureFacade.isSignatureValid()) {
                viewHolder.isSigned.setText(getContext().getText(R.string.signature_valid));
                viewHolder.isSigned.setTextColor(Color.rgb(80, 155, 0));
            } else {
                viewHolder.isSigned.setText(getContext().getText(R.string.signature_invalid));
                viewHolder.isSigned.setTextColor(Color.RED);
            }
            viewHolder.removeSignature.setOnClickListener(new RemoveSignatureListener(position, personInfo));
        }
        return convertView;
    }

    private class RemoveSignatureListener implements View.OnClickListener {

        private int position;
        private String personInfo;

        RemoveSignatureListener(int position, String personInfo) {
            this.position = position;
            this.personInfo = personInfo;
        }

        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.bdoc_remove_confirm_title);

            String confirmMessage = getContext().getString(R.string.signature_remove_confirm_message);
            confirmMessage = String.format(confirmMessage, personInfo);

            builder.setMessage(confirmMessage);

            builder.setPositiveButton(R.string.confirm_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SignatureFacade signatureFacade = getItem(position);
                    containerFacade = ContainerBuilder.aContainer(getContext()).fromExistingContainer(containerFacade.getContainerFile()).build();
                    containerFacade.removeSignature(position);
                    notificationUtil.showSuccessMessage(getContext().getText(R.string.signature_removed));
                    remove(signatureFacade);
                    notifyDataSetChanged();
                    containerSignaturesFragment.calculateFragmentHeight();
                }
            }).setNegativeButton(R.string.cancel_button, null);

            builder.create().show();
        }
    }
}
