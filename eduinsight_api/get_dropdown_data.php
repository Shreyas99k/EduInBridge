<?php
header('Content-Type: application/json');
$conn = new mysqli("localhost", "YOUR_DB_USER", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) {
    die(json_encode(["status" => "error", "message" => "Connection failed"]));
}

$institution_id = $_GET['institution_id'] ?? 0;

$response = [
    "departments" => [],
    "subjects" => [],
    "teachers_full" => [] // Use this key to send full teacher objects
];

if ($institution_id > 0) {
    // 1. Get Departments
    $dept_sql = "SELECT id, name FROM departments WHERE institution_id = $institution_id";
    $dept_res = $conn->query($dept_sql);
    while ($row = $dept_res->fetch_assoc()) {
        $response["departments"][] = $row;
    }

    // 2. Get Subjects
    $sub_sql = "SELECT id, department_id, subject_name FROM subjects WHERE department_id IN (SELECT id FROM departments WHERE institution_id = $institution_id)";
    $sub_res = $conn->query($sub_sql);
    while ($row = $sub_res->fetch_assoc()) {
        $response["subjects"][] = $row;
    }

    // 3. Get Teachers with Branch (Department Name), department_id, and is_online status
    $teacher_sql = "SELECT id, name, branch, department_id, is_online FROM users WHERE institution_id = $institution_id AND role = 'teacher' AND status = 'active'";
    $teacher_res = $conn->query($teacher_sql);
    while ($row = $teacher_res->fetch_assoc()) {
        $response["teachers_full"][] = $row;
    }
}

echo json_encode($response);
$conn->close();
?>