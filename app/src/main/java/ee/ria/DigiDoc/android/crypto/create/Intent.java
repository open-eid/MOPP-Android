package ee.ria.DigiDoc.android.crypto.create;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.Certificate;
import ee.ria.DigiDoc.android.utils.mvi.MviAction;
import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

interface Intent extends MviIntent, MviAction {

    @AutoValue
    abstract class InitialIntent implements Intent {

        static InitialIntent create() {
            return new AutoValue_Intent_InitialIntent();
        }
    }

    @AutoValue
    abstract class RecipientsAddButtonClickIntent implements Intent {

        abstract String cryptoCreateScreenId();

        static RecipientsAddButtonClickIntent create(String cryptoCreateScreenId) {
            return new AutoValue_Intent_RecipientsAddButtonClickIntent(cryptoCreateScreenId);
        }
    }

    @AutoValue
    abstract class RecipientsSearchIntent implements Intent {

        abstract String query();

        static RecipientsSearchIntent create(String query) {
            return new AutoValue_Intent_RecipientsSearchIntent(query);
        }
    }

    @AutoValue
    abstract class RecipientAddIntent implements Intent {

        abstract ImmutableList<Certificate> recipients();

        abstract Certificate recipient();

        static RecipientAddIntent create(ImmutableList<Certificate> recipients,
                                         Certificate recipient) {
            return new AutoValue_Intent_RecipientAddIntent(recipients, recipient);
        }
    }

    @AutoValue
    abstract class RecipientRemoveIntent implements Intent {

        abstract ImmutableList<Certificate> recipients();

        abstract Certificate recipient();

        static RecipientRemoveIntent create(ImmutableList<Certificate> recipients,
                                         Certificate recipient) {
            return new AutoValue_Intent_RecipientRemoveIntent(recipients, recipient);
        }
    }
}
