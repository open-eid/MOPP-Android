package ee.ria.aidl.token.tokenaidlservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

import ee.ria.aidl.token.tokenaidllibrary.CertificateResultListener;
import ee.ria.aidl.token.tokenaidllibrary.PersonalFileResultListener;
import ee.ria.aidl.token.tokenaidllibrary.PinActionsListener;
import ee.ria.aidl.token.tokenaidllibrary.SignResultListener;
import ee.ria.aidl.token.tokenaidllibrary.TokenAidlInterface;

public class TokenService extends Service {

    private static final String UNKNOWN_REASON = "Unknown";

    private SMInterface sminterface = null;
    private EstEidToken eidToken;

    public TokenService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sminterface != null) {
            sminterface.close();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        connectToReader();
        Log.i("REMOTE_SERVICE", "Connecting");
        return new TokenAidlInterface.Stub() {
            @Override
            public void changePin1(PinActionsListener listener, String oldPin, String newPin) throws RemoteException {
                changePin(listener, Token.PinType.PIN1, oldPin, newPin);
            }

            @Override
            public void changePin2(PinActionsListener listener, String oldPin, String newPin) throws RemoteException {
                changePin(listener, Token.PinType.PIN2, oldPin, newPin);
            }

            @Override
            public void changePuk(PinActionsListener listener, String oldPuk, String newPuk) throws RemoteException {
                changePin(listener, Token.PinType.PUK, oldPuk, newPuk);
            }

            @Override
            public void unblockPin1(PinActionsListener listener, String puk) throws RemoteException {
                unblockPin(listener, Token.PinType.PIN1, puk);
            }

            @Override
            public void unblockPin2(PinActionsListener listener, String puk) throws RemoteException {
                unblockPin(listener, Token.PinType.PIN2, puk);
            }

            @Override
            public void sign(final SignResultListener listener, String pin2, String hashToSignHex) throws RemoteException {
                eidToken.setSignListener(new Token.SignListener() {
                    //TODO: handle remote exceptions properly
                    @Override
                    public void onSignResponse(byte[] signature) {
                        try {
                            listener.onSignSuccess(Util.toHex(signature));
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    @Override
                    public void onSignError(String msg) {
                        try {
                            listener.onSignFailed(msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                });
                eidToken.sign(Token.PinType.PIN2, Util.fromHex(hashToSignHex), pin2);
            }

            @Override
            public void readPersonalFile(final PersonalFileResultListener listener) throws RemoteException {
                eidToken.setPersonalFileListener(new Token.PersonalFileListener() {
                    //TODO: handle remote exceptions properly
                    @Override
                    public void onPersonalFileResponse(SparseArray<String> result) {
                        try {
                            //TODO: use a Parcelable for personal file
                            listener.onPersonalFileResponse(result.toString());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    @Override
                    public void onPersonalFileError(String msg) {
                        try {
                            listener.onPersonalFileError(msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                });
                eidToken.readPersonalFile();
            }

            @Override
            public void readSignCertificateInHex(CertificateResultListener listener) throws RemoteException {
                readCertificateInHex(Token.CertType.CertSign, listener);
            }

            @Override
            public void readAuthCertificateInHex(CertificateResultListener listener) throws RemoteException {
                readCertificateInHex(Token.CertType.CertAuth, listener);
            }
        };
    }

    private void changePin(PinActionsListener listener, Token.PinType pinType, String oldPin, String newPin) throws RemoteException {
        try {
            notifyListener(listener, eidToken.changePin(oldPin.getBytes(), newPin.getBytes(), pinType));
        } catch (Exception e) {
            listener.onPinActionFailed(e.getMessage());
        }

    }

    private void unblockPin(PinActionsListener listener, Token.PinType pinType, String puk) throws RemoteException {
        try {
            notifyListener(listener, eidToken.unblockPin(puk.getBytes(), pinType));
        } catch (Exception e) {
            listener.onPinActionFailed(e.getMessage());
        }
    }

    private void readCertificateInHex(Token.CertType type, final CertificateResultListener listener) {
        eidToken.setCertListener(new Token.CertListener() {
            //TODO: handle remote exceptions properly
            @Override
            public void onCertificateResponse(Token.CertType type, byte[] cert) {
                try {
                    listener.onCertifiacteRequestSuccess(Util.toHex(cert));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onCertificateError(String msg) {
                try {
                    listener.onCertifiacteRequestFailed(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
        eidToken.readCert(type);
    }

    private void notifyListener(PinActionsListener listener, boolean success) throws RemoteException {
        if (success) {
            listener.onPinActionSuccessful();
        } else {
            listener.onPinActionFailed(UNKNOWN_REASON);
        }
    }

    private void connectToReader() {
        sminterface = SMInterface.getInstance(this, SMInterface.ACS);
        if (sminterface == null) {
            return;
        }
        sminterface.connect(new SMInterface.Connected() {
            @Override
            public void connected() {
                Log.i("SIMINTERFACE_CONNECT", "connected!");
            }
        });
        eidToken = new EstEidToken(sminterface);
    }

}
