# Privacy design record

## Data flow

1. The person taps **Escanear código**.
2. Android asks for the camera permission when necessary.
3. Camera frames are supplied to ZXing in memory for local decoding.
4. The decoded string is displayed locally. It can be copied or, for a valid
   HTTP(S) URL only, passed to the device's browser after another explicit tap.

No application network permission is declared. Ethic QR Scanner does not store a
scan history, camera frame, account identifier, analytics event or advertisement
identifier.

The optional photo feature receives one image URI from Android's system picker,
decodes it in memory, and releases it after the result. It does not request
storage permission or retain a copy.

## Safety boundary

An HTTP(S) link can still lead to a malicious website. This application does
not claim to determine whether a link is safe, does not query reputation
services, and does not open it automatically. The final decision belongs to the
person using the device.

## Permission

`android.permission.CAMERA` is required only to use the scanning feature. It
is declared in the manifest and requested at runtime only after the person
chooses to scan.
