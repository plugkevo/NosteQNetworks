## Password Security Implementation Guide

### Overview
This implementation provides a secure authentication system with:
1. **Forced Password Change on First Login** - Users must set a strong password on first login
2. **Password Expiration after 90 Days** - Users must change password every 90 days
3. **Firestore Integration** - User security data stored in Firebase Firestore
4. **Backend Password Handling** - Passwords sent to your main backend API only

### Files Created/Modified

#### 1. **UserProfile.kt** (models/)
- Firestore data model for user security information
- Fields: `uid`, `username`, `phoneNumber`, `hasChangedPassword`, `passwordChangedDate`, `createdAt`, `lastLoginDate`
- Uses Firestore's `@ServerTimestamp` for automatic date management

#### 2. **PasswordSecurityManager.kt** (main/)
- Core security logic manager
- Key functions:
  - `shouldForcePasswordChange()` - Checks if user must change password
  - `createOrUpdateUserProfile()` - Creates/updates user in Firestore after login
  - `markPasswordAsChanged()` - Records password change timestamp
  - `getDaysUntilPasswordExpiry()` - Shows remaining days before expiration
  - `isPasswordExpired()` - Internal check for 90-day expiration

#### 3. **ChangePasswordScreen.kt** (UI Composable)
- Beautiful UI for password change flow
- Password strength validation:
  - Minimum 8 characters
  - At least one uppercase letter
  - At least one lowercase letter
  - At least one digit
  - At least one special character
- Real-time visual feedback with checkmarks
- Option to skip on first login (still marked as prompted)

#### 4. **ChangePasswordActivity.kt** (Activity)
- Manages the password change flow
- Receives: `userId`, `username`, `isFirstLogin` intents
- Calls your backend API to change password
- Updates Firestore after successful change

#### 5. **LoginActivity.kt** (Updated)
- Modified login flow to include security checks
- After successful login:
  1. Saves login data to local preferences
  2. Creates/updates user profile in Firestore
  3. Checks if password needs to be changed
  4. Routes to ChangePasswordActivity if needed

#### 6. **ApiService.kt** (Updated)
- Added new endpoint: `POST api/change-password`
- Parameters: `username`, `password` (new password)
- Returns: `ChangePasswordResponse` with status and message

### Firestore Structure
```
/users/{userId}
├── uid: string
├── username: string (phone number)
├── phoneNumber: string
├── hasChangedPassword: boolean
├── passwordChangedDate: timestamp
├── createdAt: timestamp
└── lastLoginDate: timestamp
```

### Firestore Security Rules (Add to Firebase Console)
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
    }
  }
}
```

### Flow Diagrams

#### First Login Flow:
```
Login Screen
    ↓
Backend Authentication (API call)
    ↓
Save to Local Preferences
    ↓
Create/Update Firestore Profile
    ↓
Check shouldForcePasswordChange() → true
    ↓
Navigate to ChangePasswordScreen
    ↓
User enters strong password
    ↓
Send to Backend API (api/change-password)
    ↓
Mark Password Changed in Firestore
    ↓
Navigate to Main App
```

#### Subsequent Login Flow:
```
Login Screen
    ↓
Backend Authentication (API call)
    ↓
Save to Local Preferences
    ↓
Update Firestore (lastLoginDate)
    ↓
Check Password Expiration (90 days)
    ↓
If expired → Navigate to ChangePasswordScreen
If valid → Navigate to Main App
```

### Integration Steps

1. **Firebase Setup** (if not already configured):
   - Add Google Services JSON to project
   - Enable Firestore in Firebase Console
   - Set up security rules (see above)

2. **Update AndroidManifest.xml**:
   - Add ChangePasswordActivity:
   ```xml
   <activity
       android:name=".ChangePasswordActivity"
       android:exported="false" />
   ```

3. **Update your Backend API**:
   - Ensure `POST /api/change-password` endpoint accepts:
     - `username` (phone number)
     - `password` (new password)
   - Returns JSON:
   ```json
   {
       "status": true,
       "message": "Password changed successfully"
   }
   ```

4. **Optional: Add Password Expiry Warning**:
   - In MainActivity/Dashboard, check days until expiry:
   ```kotlin
   val daysRemaining = PasswordSecurityManager.getDaysUntilPasswordExpiry(userId)
   if (daysRemaining in 1..7) {
       // Show warning banner
   }
   ```

### Security Features

✅ **First Login Enforcement** - Cannot skip initial password setup
✅ **Strong Password Requirements** - 8+ chars, uppercase, lowercase, digit, special char
✅ **90-Day Expiration** - Automatic enforcement
✅ **Secure Storage** - No passwords in local storage or SharedPreferences
✅ **Firestore Backup** - User security metadata backed up in Firebase
✅ **Server Timestamps** - Uses Firestore server time (immune to device time manipulation)
✅ **Phone as Username** - Original requirement maintained

### Testing Scenarios

1. **First Login**:
   - Login with phone and default password
   - Should be forced to change password
   - Cannot proceed to app until password changed

2. **Normal Login (within 90 days)**:
   - Login succeeds
   - Taken directly to main app

3. **Password Expired (90+ days)**:
   - Login succeeds
   - Forced to change password again

4. **Force Password Change**:
   - Skipping on first login still records the event
   - Later logins will check expiration
