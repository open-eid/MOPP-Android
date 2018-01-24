package ee.ria.DigiDoc.android.utils;

import android.os.Parcel;
import android.os.Parcelable;

public final class ViewSavedState implements Parcelable {

    public interface ParcelConsumer {
        void accept(Parcel parcel);
    }

    public static Parcelable onSaveInstanceState(Parcelable superState, ParcelConsumer onSave) {
        Parcel parcel = Parcel.obtain();
        onSave.accept(parcel);
        parcel.setDataPosition(0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return new ViewSavedState(superState, bytes);
    }

    public static Parcelable onRestoreInstanceState(Parcelable state, ParcelConsumer onRestore) {
        ViewSavedState savedState = (ViewSavedState) state;
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(savedState.state, 0, savedState.state.length);
        parcel.setDataPosition(0);
        onRestore.accept(parcel);
        parcel.recycle();
        return savedState.superState;
    }

    private final Parcelable superState;
    private final byte[] state;

    private ViewSavedState(Parcelable superState, byte[] state) {
        this.superState = superState;
        this.state = state;
    }

    private ViewSavedState(Parcel source, ClassLoader loader) {
        superState = source.readParcelable(loader);
        state = source.createByteArray();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(superState, flags);
        dest.writeByteArray(state);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ViewSavedState> CREATOR = new ClassLoaderCreator<ViewSavedState>() {
        @Override
        public ViewSavedState createFromParcel(Parcel source, ClassLoader loader) {
            return new ViewSavedState(source, loader);
        }
        @Override
        public ViewSavedState createFromParcel(Parcel source) {
            return createFromParcel(source, null);
        }
        @Override
        public ViewSavedState[] newArray(int size) {
            return new ViewSavedState[size];
        }
    };
}
