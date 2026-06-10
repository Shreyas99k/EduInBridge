<?php
header('Content-Type: application/json');
$conn = new mysqli("localhost", "YOUR_DB_USER", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) {
    die(json_encode(["status" => "error", "message" => "Connection failed"]));
}

$inst_id = (int)($_GET['institution_id'] ?? $_POST['institution_id'] ?? 0);
$dept_id = (int)($_GET['department_id'] ?? $_POST['department_id'] ?? 0);
$action = $_POST['action'] ?? '';
$type = $_POST['type'] ?? '';
$name = $_POST['name'] ?? '';

// Handle DELETE operations
if (!empty($action) && $action === "delete") {
    if ($type === "department") {
        // Delete department by name
        $name = $conn->real_escape_string($name);
        
        // First delete all subjects in this department
        $stmt1 = $conn->prepare("DELETE FROM subjects WHERE department_id = (SELECT id FROM departments WHERE name = ? AND institution_id = ?)");
        if ($stmt1) {
            $stmt1->bind_param("si", $name, $inst_id);
            $stmt1->execute();
            $stmt1->close();
        }
        
        // Then delete the department
        $stmt = $conn->prepare("DELETE FROM departments WHERE name = ? AND institution_id = ?");
        if (!$stmt) {
            echo json_encode(["status" => "error", "message" => "Prepare failed: " . $conn->error]);
            exit;
        }
        
        $stmt->bind_param("si", $name, $inst_id);
        if ($stmt->execute()) {
            echo json_encode(["status" => "success", "message" => "Department deleted successfully"]);
        } else {
            echo json_encode(["status" => "error", "message" => "Failed to delete department"]);
        }
        $stmt->close();
    } elseif ($type === "subject") {
        // Delete subject by name and department_id
        $name = $conn->real_escape_string($name);
        
        $stmt = $conn->prepare("DELETE FROM subjects WHERE subject_name = ? AND department_id = ?");
        if (!$stmt) {
            echo json_encode(["status" => "error", "message" => "Prepare failed: " . $conn->error]);
            exit;
        }
        
        $stmt->bind_param("si", $name, $dept_id);
        if ($stmt->execute()) {
            echo json_encode(["status" => "success", "message" => "Subject deleted successfully"]);
        } else {
            echo json_encode(["status" => "error", "message" => "Failed to delete subject"]);
        }
        $stmt->close();
    }
} elseif (!empty($action) && $action === "add") {
    // ...existing code...
    if ($type === "department") {
        $dept_name = $conn->real_escape_string($_POST['name'] ?? '');
        if (empty($dept_name)) {
            echo json_encode(["success" => false, "message" => "Department name is required"]);
            exit;
        }
        
        // Insert department
        $stmt = $conn->prepare("INSERT INTO departments (institution_id, name) VALUES (?, ?)");
        if (!$stmt) {
            echo json_encode(["success" => false, "message" => "Prepare failed: " . $conn->error]);
            exit;
        }
        
        $stmt->bind_param("is", $inst_id, $dept_name);
        
        if ($stmt->execute()) {
            $new_dept_id = $conn->insert_id;
            
            // Handle subjects if provided
            $subjects = $_POST['subjects'] ?? '';
            if (!empty($subjects)) {
                $subject_array = explode("|", $subjects);
                foreach ($subject_array as $subject) {
                    $subject = trim($subject);
                    if (!empty($subject)) {
                        $subject = $conn->real_escape_string($subject);
                        $stmt2 = $conn->prepare("INSERT INTO subjects (department_id, subject_name) VALUES (?, ?)");
                        if ($stmt2) {
                            $stmt2->bind_param("is", $new_dept_id, $subject);
                            $stmt2->execute();
                            $stmt2->close();
                        }
                    }
                }
            }
            
            echo json_encode(["success" => true, "department_id" => $new_dept_id, "message" => "Department added successfully"]);
        } else {
            echo json_encode(["success" => false, "message" => "Failed to add department: " . $stmt->error]);
        }
        $stmt->close();
    } elseif ($type === "subject") {
        $dept_id = (int)($_POST['department_id'] ?? 0);
        $subjects = $_POST['name'] ?? '';
        
        if ($dept_id <= 0 || empty($subjects)) {
            echo json_encode(["success" => false, "message" => "Department ID and subject names are required"]);
            exit;
        }
        
        $subject_array = explode("|", $subjects);
        $added_count = 0;
        
        foreach ($subject_array as $subject) {
            $subject = trim($subject);
            if (!empty($subject)) {
                $subject = $conn->real_escape_string($subject);
                $stmt = $conn->prepare("INSERT INTO subjects (department_id, subject_name) VALUES (?, ?)");
                
                if ($stmt) {
                    $stmt->bind_param("is", $dept_id, $subject);
                    
                    if ($stmt->execute()) {
                        $added_count++;
                    }
                    $stmt->close();
                }
            }
        }
        
        echo json_encode(["success" => true, "added_count" => $added_count, "message" => "Subjects added successfully"]);
    }
} elseif ($dept_id > 0) {
    // Mode: Get subjects for a specific department (GET request)
    $stmt = $conn->prepare("SELECT subject_name FROM subjects WHERE department_id = ?");
    if (!$stmt) {
        echo json_encode(["status" => "error", "message" => "Prepare failed: " . $conn->error]);
        exit;
    }
    
    $stmt->bind_param("i", $dept_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $subjects = [];
    while ($row = $result->fetch_assoc()) {
        $subjects[] = $row['subject_name'];
    }
    echo json_encode(["status" => "success", "subjects" => $subjects]);
    $stmt->close();
} else {
    // Mode: Get all departments for this institution (GET request)
    $stmt = $conn->prepare("SELECT id, name FROM departments WHERE institution_id = ?");
    if (!$stmt) {
        echo json_encode(["status" => "error", "message" => "Prepare failed: " . $conn->error]);
        exit;
    }
    
    $stmt->bind_param("i", $inst_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $departments = [];
    while ($row = $result->fetch_assoc()) {
        $departments[] = $row;
    }
    echo json_encode(["status" => "success", "departments" => $departments]);
    $stmt->close();
}

$conn->close();
?>


