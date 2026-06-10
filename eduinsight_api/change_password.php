<?php
include 'db.php';

if($_SERVER['REQUEST_METHOD'] == 'POST'){
    $user_id = $_POST['user_id'];
    $old_pass = $_POST['old_pass'];
    $new_pass = $_POST['new_pass'];

    // 1. Check if old password is correct
    $check_sql = "SELECT password FROM users WHERE id = ?";
    $stmt = $conn->prepare($check_sql);
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $row = $result->fetch_assoc();

    if($row && $row['password'] == $old_pass){
        // 2. Update to new password
        $update_sql = "UPDATE users SET password = ? WHERE id = ?";
        $upd_stmt = $conn->prepare($update_sql);
        $upd_stmt->bind_param("si", $new_pass, $user_id);
        
        if($upd_stmt->execute()){
            echo "success";
        } else {
            echo "Failed to update";
        }
    } else {
        echo "Current password is incorrect";
    }
}
?>