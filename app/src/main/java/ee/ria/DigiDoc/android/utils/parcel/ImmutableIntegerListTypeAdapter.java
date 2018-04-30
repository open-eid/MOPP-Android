package ee.ria.DigiDoc.android.utils.parcel;

import android.os.Parcel;

import com.google.common.collect.ImmutableList;
import com.ryanharter.auto.value.parcel.TypeAdapter;

public final class ImmutableIntegerListTypeAdapter implements TypeAdapter<ImmutableList<Integer>> {

    @Override
    public ImmutableList<Integer> fromParcel(Parcel in) {
        int[] data = in.createIntArray();
        ImmutableList.Builder<Integer> builder = ImmutableList.builder();
        for (int item : data) {
            builder.add(item);
        }
        return builder.build();
    }

    @Override
    public void toParcel(ImmutableList<Integer> value, Parcel dest) {
        int[] data = new int[value.size()];
        for (int i = 0; i < value.size(); i++) {
            data[i] = value.get(i);
        }
        dest.writeIntArray(data);
    }
}
