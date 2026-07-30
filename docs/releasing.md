# WMAID release lanes

## Current showcase lane

The GitHub Actions workflow **Manual Android build** runs only when a maintainer
starts it. It creates phone and watch APKs signed with the same private demo key
on every run, so a newer APK can update an earlier showcase install.

By default, the APKs remain a private workflow artifact. Selecting
`publish_release` explicitly creates a public GitHub prerelease. This is an
opt-in publication action and is not a production deployment.

The workflow expects these GitHub Actions secrets:

- `WMAID_DEMO_KEYSTORE_BASE64`
- `WMAID_DEMO_KEYSTORE_PASSWORD`
- `WMAID_DEMO_KEY_ALIAS`
- `WMAID_DEMO_KEY_PASSWORD`

Secret values and the signing keystore must never be committed.

## Future device-update lane

Google Play internal testing is the intended update path for the physical
Samsung phone and Galaxy Watch Ultra. It supplies normal background update
delivery without an attached debugger. This lane remains held until a Play
Console developer account and release signing decision are available.

The production Firebase relay and Play publication are separate gates. A
successful signed APK build does not mean either service is deployed.
