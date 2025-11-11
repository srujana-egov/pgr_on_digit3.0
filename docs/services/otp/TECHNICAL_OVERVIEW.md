# OTP Service

##  Purpose

Provide a simple, secure, and configurable service to generate, deliver, verify, and manage OTPs for user flows (login, MFA, password reset, transaction confirmation). Delivery is delegated to an external Notification Service (SMS / Email).

## High-level flows

1. Generate OTP — create OTP, persist metadata, call Notification Service to deliver.

2. Resend OTP — re-send same OTP (or new OTP depending on config) after resend_delay.

3. Verify OTP — verify user-supplied OTP; on success mark used and emit event.

4. Invalidate/Expire OTP — automatically or manually mark OTP invalid.

5. Rate-limit & Lockout — protect against abuse by rate-limiting requests and attempts.

## Configurable parameters (per-tenant / per-flow)

* otp_length (default: 6) — number of characters (numeric only by default).

* otp_charset — e.g., digits, alphanumeric, base32 (default: digits).

* otp_lifetime_seconds (default: 300) — expiry time.

* resend_delay_seconds (default: 30) — earliest time before resend allowed.

* max_resend_count (default: 3) — maximum resends allowed per OTP request.

* max_verify_attempts (default: 5) — attempts before invalidating OTP and/or locking.

* max_generate_per_destination_per_hour — rate cap per phone/email per hour.

* max_generate_per_ip_per_hour — optional IP-based caps.

* use_single_active_otp (true/false) — whether new OTP invalidates previous ones for same purpose/destination.

* hash_algorithm — algorithm for storing OTPs (e.g., HMAC-SHA256 with server secret, or PBKDF2).

* notification_channel_priority — e.g., ["sms","email"].

* otp_purpose — e.g., login, mfa, password_reset, transaction. Configurable policies per purpose.



## Data model (core)

### OTPRecord

* id (UUID)

* destination (string) — phone number or email, canonicalized

* destination_type (phone/email/push)

* purpose (string)

* otp_hash (string) — hashed or HMAC’d OTP

* salt (optional)

* created_at (timestamp)

* expires_at (timestamp)

* attempts (int) — verify attempts used

* resend_count (int)

* last_sent_at (timestamp)

* status (pending, verified, expired, invalidated, locked)

* client_ip (string) — optional for auditing

* metadata (map) — e.g., device info, correlation id

* tenant_id (string) — if multitenant

Note: never store OTP in plaintext. Store hashed OTP (HMAC with secret and per-record nonce or PBKDF2 with salt).



## Security & privacy

* Never log OTP values in plaintext. Scrub logs and traces.

* Store hashed OTPs only. Use keyed HMAC or PBKDF2/Argon2 with per-record salt.

* Protect secrets (HMAC key) in a secrets manager (KMS, Vault).

* Enforce TLS for all inbound/outbound connections.

* Rate-limit generational and verification attempts to avoid enumeration.

* Use constant-time equality checks to avoid timing attacks.

* Short expiry windows (default 5 minutes).


## Rate-Limiting
| Control | Behavior |
|---------|----------|
| Per destination | max_generate_per_destination_per_hour |
| Per IP | max_generate_per_ip_per_hour |
| Wrong attempts | Lock and invalidate OTP |
| Resend | Allowed only after `resend_delay_seconds` |

If limits exceeded ⇒ return `429 Too Many Requests`.


## Events / Audit Logging
Events emitted (based on on/off flag) (to Kafka/Redis):
- `otp.generated`
- `otp.sent`
- `otp.resent`
- `otp.verified`
- `otp.failed`
- `otp.locked`
- `otp.invalidated`

Do not include OTP value in events/logs.


## 10. Acceptance Criteria

### Generate
- Must return `request_id`, `expires_at`, `resend_allowed_after`.
- Must enforce rate-limit.

### Resend
- Must enforce `max_resend_count`.
- Must check `resend_delay_seconds`.

### Verify
- Correct OTP ⇒ `status=verified`.
- Incorrect OTP increments `attempts`.
- After `max_verify_attempts`, OTP is `locked`.

### Security
- OTP not loggable or retrievable.
- Hash stored only.


## Edge Cases
- Resend allowed only when `resend_delay_seconds` passed.
- Optionally allow regenerate vs resend same OTP (configurable).
- Invalidate old OTPs when `use_single_active_otp=true`.


## Implementation Notes
- Redis recommended for OTP storage (short TTL) + TTL-based expiry.
- Postgres or any DB for long-term audit trail.
- Notification delivery can be sync/async based on flag.


