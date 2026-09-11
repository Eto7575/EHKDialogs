package com.ehk.dialogs;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.YailList;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Calendar;

@DesignerComponent(
    version = 3,
    description = "Advanced Custom System Dialogs Extension for App Inventor & Niotron",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "aiwebres/icon.png"
)
@SimpleObject(external = true)
public class EHKDialogs extends AndroidNonvisibleComponent {

    private final Activity activity;
    private final Context context;
    private ProgressDialog activeProgressDialog;

    public EHKDialogs(ComponentContainer container) {
        super(container.$form());
        this.activity = (Activity) container.$context();
        this.context = container.$context();
    }

    // ==========================================
    // EVENTS
    // ==========================================

    @SimpleEvent(description = "Event triggered when a new password is set successfully.")
    public void PasswordSet(String output, String title, String message) {
        EventDispatcher.dispatchEvent(this, "PasswordSet", output, title, message);
    }

    @SimpleEvent(description = "Event triggered when password validation or request is completed.")
    public void PasswordEntered(boolean isCorrect, String title, String message) {
        EventDispatcher.dispatchEvent(this, "PasswordEntered", isCorrect, title, message);
    }

    @SimpleEvent(description = "Event triggered when a phone number is entered.")
    public void PhoneNumberEntered(String output, String title, String message, String format) {
        EventDispatcher.dispatchEvent(this, "PhoneNumberEntered", output, title, message, format);
    }

    @SimpleEvent(description = "Event triggered when an email address is entered.")
    public void EmailEntered(String output, String title, String message) {
        EventDispatcher.dispatchEvent(this, "EmailEntered", output, title, message);
    }

    @SimpleEvent(description = "Event triggered when progress bar dialog is canceled by user.")
    public void ProgressCanceled(float currentProgress, String title, String message) {
        EventDispatcher.dispatchEvent(this, "ProgressCanceled", currentProgress, title, message);
    }

    @SimpleEvent(description = "Event triggered after general text input dialog submission.")
    public void AfterTextInput(String result) {
        EventDispatcher.dispatchEvent(this, "AfterTextInput", result);
    }

    @SimpleEvent(description = "Event triggered after confirm dialog selection.")
    public void AfterConfirmDialog(boolean confirmed) {
        EventDispatcher.dispatchEvent(this, "AfterConfirmDialog", confirmed);
    }

    @SimpleEvent(description = "Event triggered after selecting an item from list selection dialog.")
    public void AfterListSelection(int index, String item) {
        EventDispatcher.dispatchEvent(this, "AfterListSelection", index, item);
    }

    @SimpleEvent(description = "Event triggered when date is selected.")
    public void AfterDateSelected(int year, int month, int day) {
        EventDispatcher.dispatchEvent(this, "AfterDateSelected", year, month, day);
    }

    @SimpleEvent(description = "Event triggered when time is selected.")
    public void AfterTimeSelected(int hour, int minute) {
        EventDispatcher.dispatchEvent(this, "AfterTimeSelected", hour, minute);
    }

    @SimpleEvent(description = "Event triggered when message dialog is closed.")
    public void AfterMessageClosed() {
        EventDispatcher.dispatchEvent(this, "AfterMessageClosed");
    }

    // ==========================================
    // FUNCTIONS / METHODS
    // ==========================================

    @SimpleFunction(description = "Shows a password creation dialog with validation, SHA-256 hashing, TinyDB saving, PIN mode, and eye toggle.")
    public void ShowPasswordSetupDialog(
            final String title,
            final String message,
            final int minChars,
            final int maxChars,
            final String charErrorMsg,
            final boolean requireCase,
            final String caseErrorMsg,
            final boolean requireDigit,
            final String digitErrorMsg,
            final String sha256Salt,
            final Object tinyDbInstance,
            final String tinyDbTag,
            final boolean isPin) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(title);
                builder.setMessage(message);

                LinearLayout layout = createInputFieldContainer();
                final EditText input = new EditText(context);
                input.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

                int inputType = isPin ?
                        (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD) :
                        (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                input.setInputType(inputType);

                if (maxChars > 0) {
                    input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxChars)});
                }

                final TextView eyeToggle = createEyeToggleButton(input, isPin);
                layout.addView(input);
                layout.addView(eyeToggle);
                builder.setView(layout);

                builder.setPositiveButton("OK", null);
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                final AlertDialog dialog = builder.create();
                dialog.show();

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String rawPass = input.getText().toString();

                        if (rawPass.length() < minChars || (maxChars > 0 && rawPass.length() > maxChars)) {
                            ShowCustomToast(charErrorMsg.isEmpty() ? "Length error" : charErrorMsg, false);
                            return;
                        }

                        if (requireCase && !isPin) {
                            boolean hasUpper = !rawPass.equals(rawPass.toLowerCase());
                            boolean hasLower = !rawPass.equals(rawPass.toUpperCase());
                            if (!hasUpper || !hasLower) {
                                ShowCustomToast(caseErrorMsg.isEmpty() ? "Uppercase and Lowercase letters required" : caseErrorMsg, false);
                                return;
                            }
                        }

                        if (requireDigit) {
                            boolean hasDigit = rawPass.matches(".*\\d.*");
                            if (!hasDigit) {
                                ShowCustomToast(digitErrorMsg.isEmpty() ? "At least one number is required" : digitErrorMsg, false);
                                return;
                            }
                        }

                        String finalOutput = rawPass;
                        if (sha256Salt != null && !sha256Salt.equalsIgnoreCase("false") && !sha256Salt.isEmpty()) {
                            finalOutput = computeSha256(rawPass, sha256Salt);
                        }

                        saveToTinyDb(tinyDbInstance, tinyDbTag, finalOutput);
                        dialog.dismiss();
                        PasswordSet(finalOutput, title, message);
                    }
                });
            }
        });
    }

    @SimpleFunction(description = "Shows a password prompt dialog to request/verify password against TinyDB or SHA-256 salt.")
    public void ShowPasswordRequestDialog(
            final String title,
            final String message,
            final boolean verify,
            final Object tinyDbInstance,
            final String tinyDbTag,
            final String sha256Salt,
            final boolean isPin) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(title);
                builder.setMessage(message);

                LinearLayout layout = createInputFieldContainer();
                final EditText input = new EditText(context);
                input.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

                int inputType = isPin ?
                        (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD) :
                        (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                input.setInputType(inputType);

                final TextView eyeToggle = createEyeToggleButton(input, isPin);
                layout.addView(input);
                layout.addView(eyeToggle);
                builder.setView(layout);

                builder.setPositiveButton("Verify", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String rawPass = input.getText().toString();
                        boolean isCorrect = true;

                        if (verify) {
                            String storedVal = readFromTinyDb(tinyDbInstance, tinyDbTag);
                            String enteredHash = rawPass;

                            if (sha256Salt != null && !sha256Salt.equalsIgnoreCase("false") && !sha256Salt.isEmpty()) {
                                enteredHash = computeSha256(rawPass, sha256Salt);
                            }

                            if (storedVal != null && !storedVal.isEmpty()) {
                                isCorrect = storedVal.equals(enteredHash) || storedVal.equals(rawPass);
                            } else {
                                isCorrect = false;
                            }
                        }

                        PasswordEntered(isCorrect, title, message);
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.show();
            }
        });
    }

    @SimpleFunction(description = "Shows a formatted phone number dialog using custom mask format (e.g., +** *** *** ****).")
    public void ShowPhoneNumberDialog(
            final String title,
            final String message,
            final String format,
            final Object tinyDbInstance,
            final String tinyDbTag) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(title);
                builder.setMessage(message);

                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_PHONE);
                input.setHint(format);

                input.addTextChangedListener(new TextWatcher() {
                    private boolean isUpdating = false;

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (isUpdating || format == null || format.isEmpty()) return;
                        isUpdating = true;

                        String digits = s.toString().replaceAll("[^0-9]", "");
                        StringBuilder formatted = new StringBuilder();
                        int digitIndex = 0;

                        for (int i = 0; i < format.length() && digitIndex < digits.length(); i++) {
                            char m = format.charAt(i);
                            if (m == '*') {
                                formatted.append(digits.charAt(digitIndex++));
                            } else {
                                formatted.append(m);
                            }
                        }

                        s.replace(0, s.length(), formatted.toString());
                        isUpdating = false;
                    }
                });

                builder.setView(input);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String result = input.getText().toString();
                        saveToTinyDb(tinyDbInstance, tinyDbTag, result);
                        PhoneNumberEntered(result, title, message, format);
                    }
                });

                builder.setNegativeButton("Cancel", null);
                builder.show();
            }
        });
    }

    @SimpleFunction(description = "Shows an email input dialog with optional TinyDB storage.")
    public void ShowEmailInputDialog(
            final String title,
            final String message,
            final Object tinyDbInstance,
            final String tinyDbTag) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(title);
                builder.setMessage(message);

                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                input.setHint("example@domain.com");
                builder.setView(input);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String email = input.getText().toString();
                        saveToTinyDb(tinyDbInstance, tinyDbTag, email);
                        EmailEntered(email, title, message);
                    }
                });

                builder.setNegativeButton("Cancel", null);
                builder.show();
            }
        });
    }

    @SimpleFunction(description = "Shows a system native progress bar dialog with unit and cancelable option.")
    public void ShowProgressBarDialog(
            final String title,
            final String message,
            final float totalProgress,
            final String progressUnit,
            final boolean unitIsPrefix,
            final boolean cancelable) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DismissProgressBarDialog(title, message);

                activeProgressDialog = new ProgressDialog(context);
                activeProgressDialog.setTitle(title);
                activeProgressDialog.setMessage(message);
                activeProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                activeProgressDialog.setMax((int) totalProgress);
                activeProgressDialog.setCancelable(cancelable);

                if (cancelable) {
                    activeProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            float current = activeProgressDialog != null ? activeProgressDialog.getProgress() : 0;
                            ProgressCanceled(current, title, message);
                        }
                    });
                }

                activeProgressDialog.show();
            }
        });
    }

    @SimpleFunction(description = "Updates the active progress bar dialog with current progress value and updated status message.")
    public void UpdateProgressBarDialog(
            final String title,
            final String message,
            final float progress,
            final String updateMessage,
            final boolean cancelable) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (activeProgressDialog != null && activeProgressDialog.isShowing()) {
                    activeProgressDialog.setProgress((int) progress);
                    if (updateMessage != null && !updateMessage.isEmpty()) {
                        activeProgressDialog.setMessage(updateMessage);
                    }
                    activeProgressDialog.setCancelable(cancelable);
                }
            }
        });
    }

    @SimpleFunction(description = "Dismisses the active progress bar dialog.")
    public void DismissProgressBarDialog(final String title, final String message) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (activeProgressDialog != null && activeProgressDialog.isShowing()) {
                    activeProgressDialog.dismiss();
                    activeProgressDialog = null;
                }
            }
        });
    }

    @SimpleFunction(description = "Shows a basic message dialog.")
    public void ShowMessageDialog(final String title, final String message, final String buttonText) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(title);
                builder.setMessage(message);
                builder.setPositiveButton(buttonText.isEmpty() ? "OK" : buttonText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AfterMessageClosed();
                    }
                });
                builder.show();
            }
        });
    }

    @SimpleFunction(description = "Shows a confirm dialog with Yes/No options.")
    public void ShowConfirmDialog(final String title, final String message, final String positiveText, final String negativeText) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(title);
                builder.setMessage(message);
                builder.setPositiveButton(positiveText.isEmpty() ? "Yes" : positiveText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AfterConfirmDialog(true);
                    }
                });
                builder.setNegativeButton(negativeText.isEmpty() ? "No" : negativeText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AfterConfirmDialog(false);
                    }
                });
                builder.show();
            }
        });
    }

    @SimpleFunction(description = "Shows a standard text input dialog.")
    public void ShowTextInputDialog(final String title, final String message, final String defaultText, final String hint) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(title);
                builder.setMessage(message);

                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                if (!defaultText.isEmpty()) input.setText(defaultText);
                if (!hint.isEmpty()) input.setHint(hint);
                builder.setView(input);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AfterTextInput(input.getText().toString());
                    }
                });

                builder.setNegativeButton("Cancel", null);
                builder.show();
            }
        });
    }

    @SimpleFunction(description = "Shows a list selection dialog.")
    public void ShowListSelectionDialog(final String title, final YailList items) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final String[] stringItems = items.toStringArray();
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(title);
                builder.setItems(stringItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AfterListSelection(which + 1, stringItems[which]);
                    }
                });
                builder.setNegativeButton("Cancel", null);
                builder.show();
            }
        });
    }

    @SimpleFunction(description = "Shows a system date picker dialog.")
    public void ShowDatePickerDialog(final String title) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Calendar c = Calendar.getInstance();
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                        AfterDateSelected(selectedYear, selectedMonth + 1, selectedDay);
                    }
                }, year, month, day);

                if (!title.isEmpty()) datePickerDialog.setTitle(title);
                datePickerDialog.show();
            }
        });
    }

    @SimpleFunction(description = "Shows a system time picker dialog.")
    public void ShowTimePickerDialog(final String title) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Calendar c = Calendar.getInstance();
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int minute = c.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minuteOfHour) {
                        AfterTimeSelected(hourOfDay, minuteOfHour);
                    }
                }, hour, minute, true);

                if (!title.isEmpty()) timePickerDialog.setTitle(title);
                timePickerDialog.show();
            }
        });
    }

    @SimpleFunction(description = "Displays a short or long toast notification.")
    public void ShowCustomToast(final String message, final boolean isLong) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private LinearLayout createInputFieldContainer() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setPadding(32, 16, 32, 16);
        return layout;
    }

    private TextView createEyeToggleButton(final EditText input, final boolean isPin) {
        final TextView eyeToggle = new TextView(context);
        eyeToggle.setText("👁");
        eyeToggle.setTextSize(18);
        eyeToggle.setPadding(16, 0, 16, 0);

        final boolean[] isVisible = {false};
        eyeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isVisible[0] = !isVisible[0];
                if (isVisible[0]) {
                    eyeToggle.setText("🙈");
                    input.setInputType(isPin ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT);
                } else {
                    eyeToggle.setText("👁");
                    input.setInputType(isPin ?
                            (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD) :
                            (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
                }
                input.setSelection(input.getText().length());
            }
        });

        return eyeToggle;
    }

    private void saveToTinyDb(Object tinyDbInstance, String tag, String value) {
        if (tinyDbInstance == null || tag == null || tag.isEmpty() || tinyDbInstance.toString().equalsIgnoreCase("false")) {
            return;
        }
        try {
            Method storeValueMethod = tinyDbInstance.getClass().getMethod("StoreValue", String.class, Object.class);
            storeValueMethod.invoke(tinyDbInstance, tag, value);
        } catch (Exception e) {
            // TinyDB reflective call fallback or ignored if non-component passed
        }
    }

    private String readFromTinyDb(Object tinyDbInstance, String tag) {
        if (tinyDbInstance == null || tag == null || tag.isEmpty() || tinyDbInstance.toString().equalsIgnoreCase("false")) {
            return null;
        }
        try {
            Method getValueMethod = tinyDbInstance.getClass().getMethod("GetValue", String.class, Object.class);
            Object result = getValueMethod.invoke(tinyDbInstance, tag, "");
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String computeSha256(String input, String salt) {
        String dataToHash = input;
        if (salt != null && !salt.equalsIgnoreCase("false") && !salt.isEmpty()) {
            dataToHash = salt + input;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return input;
        }
    }
}
