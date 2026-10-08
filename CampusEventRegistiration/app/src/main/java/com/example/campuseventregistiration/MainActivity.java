package com.example.campuseventregistiration;

import android.content.Context;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** On-device CRUD screen for student campus-event registrations. */
public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "campus_registration_prefs";
    private static final String STUDENTS_KEY = "student_event_registrations";
    private final List<StudentRegistration> registrations = new ArrayList<>();
    private LinearLayout eventsContainer;
    private TextView emptyState;
    private TextView registrationCount;
    private TextView eventCountBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        eventsContainer = findViewById(R.id.eventsContainer);
        emptyState = findViewById(R.id.emptyState);
        registrationCount = findViewById(R.id.registrationCount);
        eventCountBadge = findViewById(R.id.eventCountBadge);
        findViewById(R.id.addButton).setOnClickListener(view -> showEventDialog(null));
        loadRegistrations();
        renderRegistrations();
    }

    private void loadRegistrations() {
        registrations.clear();
        String saved = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(STUDENTS_KEY, "[]");
        try {
            JSONArray array = new JSONArray(saved);
            for (int index = 0; index < array.length(); index++) {
                registrations.add(StudentRegistration.fromJson(array.getJSONObject(index)));
            }
        } catch (JSONException ignored) {
            // Corrupt local state should not make the app unusable.
        }
    }

    private void saveRegistrations() {
        JSONArray array = new JSONArray();
        for (StudentRegistration registration : registrations) array.put(registration.toJson());
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(STUDENTS_KEY, array.toString()).apply();
    }

    private void renderRegistrations() {
        eventsContainer.removeAllViews();
        int count = registrations.size();
        registrationCount.setText(count + (count == 1 ? " student" : " students"));
        eventCountBadge.setText(count + (count == 1 ? " record" : " records"));
        emptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (StudentRegistration registration : registrations) {
            View card = inflater.inflate(R.layout.item_event, eventsContainer, false);
            bindRegistrationCard(card, registration);
            eventsContainer.addView(card);
        }
    }

    private void bindRegistrationCard(View card, StudentRegistration registration) {
        ((TextView) card.findViewById(R.id.eventInitial)).setText(registration.studentName.substring(0, 1).toUpperCase());
        ((TextView) card.findViewById(R.id.studentName)).setText(registration.studentName);
        ((TextView) card.findViewById(R.id.studentId)).setText("ID: " + registration.studentId);
        ((TextView) card.findViewById(R.id.eventName)).setText(registration.eventName);
        ((TextView) card.findViewById(R.id.studentEmail)).setText(registration.email);
        card.setOnClickListener(view -> showEventDialog(registration));
        ImageButton moreButton = card.findViewById(R.id.moreButton);
        moreButton.setOnClickListener(view -> showActionsMenu(moreButton, registration));
    }

    private void showActionsMenu(View anchor, StudentRegistration registration) {
        android.widget.PopupMenu menu = new android.widget.PopupMenu(this, anchor);
        menu.getMenu().add(Menu.NONE, 1, Menu.NONE, "Edit registration");
        menu.getMenu().add(Menu.NONE, 2, Menu.NONE, "Delete registration");
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) showEventDialog(registration);
            if (item.getItemId() == 2) confirmDelete(registration);
            return true;
        });
        menu.show();
    }

    private void showEventDialog(StudentRegistration existing) {
        View form = getLayoutInflater().inflate(R.layout.dialog_event, null);
        TextInputEditText studentNameInput = form.findViewById(R.id.inputStudentName);
        TextInputEditText studentIdInput = form.findViewById(R.id.inputStudentId);
        TextInputEditText emailInput = form.findViewById(R.id.inputStudentEmail);
        TextInputEditText eventInput = form.findViewById(R.id.inputEventName);
        boolean isEditing = existing != null;
        if (isEditing) {
            studentNameInput.setText(existing.studentName);
            studentIdInput.setText(existing.studentId);
            emailInput.setText(existing.email);
            eventInput.setText(existing.eventName);
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(isEditing ? "Update student" : "Register a student")
                .setMessage(isEditing ? "Keep this student's event registration current." : "Add a student to a campus event.")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(isEditing ? "Save changes" : "Register student", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String studentName = textOf(studentNameInput);
            String studentId = textOf(studentIdInput);
            String email = textOf(emailInput);
            String eventName = textOf(eventInput);
            if (studentName.isEmpty() || studentId.isEmpty() || email.isEmpty() || eventName.isEmpty()) {
                Toast.makeText(this, "Please complete all fields.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Enter a valid student email.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isEditing) {
                existing.studentName = studentName;
                existing.studentId = studentId;
                existing.email = email;
                existing.eventName = eventName;
            } else {
                registrations.add(new StudentRegistration(UUID.randomUUID().toString(), studentName, studentId, email, eventName));
            }
            saveRegistrations();
            renderRegistrations();
            Toast.makeText(this, isEditing ? "Student registration updated" : "Student registered", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }));
        dialog.show();
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void confirmDelete(StudentRegistration registration) {
        new AlertDialog.Builder(this)
                .setTitle("Delete registration?")
                .setMessage("Remove " + registration.studentName + " from \"" + registration.eventName + "\"?")
                .setNegativeButton("Keep", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    registrations.remove(registration);
                    saveRegistrations();
                    renderRegistrations();
                    Toast.makeText(this, "Student registration deleted", Toast.LENGTH_SHORT).show();
                }).show();
    }

    private static class StudentRegistration {
        private final String id;
        private String studentName;
        private String studentId;
        private String email;
        private String eventName;

        StudentRegistration(String id, String studentName, String studentId, String email, String eventName) {
            this.id = id;
            this.studentName = studentName;
            this.studentId = studentId;
            this.email = email;
            this.eventName = eventName;
        }

        JSONObject toJson() {
            JSONObject object = new JSONObject();
            try {
                object.put("id", id);
                object.put("studentName", studentName);
                object.put("studentId", studentId);
                object.put("email", email);
                object.put("eventName", eventName);
            } catch (JSONException ignored) { }
            return object;
        }

        static StudentRegistration fromJson(@NonNull JSONObject object) throws JSONException {
            return new StudentRegistration(object.optString("id", UUID.randomUUID().toString()), object.getString("studentName"), object.getString("studentId"), object.getString("email"), object.getString("eventName"));
        }
    }
}
