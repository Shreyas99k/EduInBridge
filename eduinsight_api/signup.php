<?php
header('Content-Type: application/json');

// 1. Database Connection
$conn = new mysqli("localhost", "root", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Database connection failed"]);
    exit();
}

// 2. Get Data from Android App (using trim to handle spaces)
$name = trim($_POST['name'] ?? "");
$email = trim($_POST['email'] ?? "");
$mobile = trim($_POST['mobile'] ?? ""); // Changed to empty string default for easier checking
$password = $_POST['password'] ?? "";
$role = $_POST['role'] ?? "";
$branch = $_POST['branch'] ?? "";
$address = $_POST['address'] ?? null;
$fcm_token = $_POST['fcm_token'] ?? "";
$institution_id = $_POST['institution_id'] ?? null;
$department_id = $_POST['department_id'] ?? null;
$status = $_POST['status'] ?? "active";

// 3. Validation
if (empty($name) || empty($email) || empty($password) || empty($role)) {
    echo json_encode(["status" => "error", "message" => "Required fields (Name, Email, Password, Role) are missing"]);
    exit();
}

// 4. Check if Email already exists
$stmt = $conn->prepare("SELECT id FROM users WHERE email = ?");
$stmt->bind_param("s", $email);
$stmt->execute();
if ($stmt->get_result()->num_rows > 0) {
    echo json_encode(["status" => "error", "message" => "Account already exists with this email"]);
    $stmt->close();
    exit();
}
$stmt->close();

// 5. Check if Mobile exists (Improved check)
if ($mobile !== "" && $mobile !== "null") {
    $stmt = $conn->prepare("SELECT id FROM users WHERE mobile = ?");
    $stmt->bind_param("s", $mobile);
    $stmt->execute();
    if ($stmt->get_result()->num_rows > 0) {
        echo json_encode(["status" => "error", "message" => "Mobile number already registered"]);
        $stmt->close();
        exit();
    }
    $stmt->close();
}

// 6. Insert New User
// Ensure mobile is stored as NULL only if it's truly empty
$mobile_val = ($mobile !== "" && $mobile !== "null") ? $mobile : null;
$inst_id_val = (!empty($institution_id) && $institution_id != "-1") ? (int)$institution_id : null;
$dept_id_val = (!empty($department_id) && $department_id != "") ? (int)$department_id : null;
$address_val = !empty($address) ? $address : null;

$sql = "INSERT INTO users (name, email, mobile, password, role, branch, address, fcm_token, institution_id, department_id, status) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

$stmt = $conn->prepare($sql);
if (!$stmt) {
    echo json_encode(["status" => "error", "message" => "SQL Prepare failed: " . $conn->error]);
    exit();
}

// Bind 11 parameters: 8 strings, 2 integers, 1 string
$stmt->bind_param("ssssssssiis", $name, $email, $mobile_val, $password, $role, $branch, $address_val, $fcm_token, $inst_id_val, $dept_id_val, $status);

if ($stmt->execute()) {
    echo json_encode(["status" => "success", "message" => "Registration Successful"]);
} else {
    echo json_encode(["status" => "error", "message" => "Registration Failed: " . $stmt->error]);
}

$stmt->close();
$conn->close();
?>