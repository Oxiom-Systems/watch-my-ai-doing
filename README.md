# WMAID — Watch My AI Doing

Your AI taps your wrist when it needs you.

WMAID is an emulator-first Android and Wear OS showcase for supervising Claude Code and Codex sessions away from the Mac. It turns waiting prompts, permission requests, overloads, and failures into concise phone and watch notifications that can be answered or handed off to the corresponding mobile session.

## Project status

The product contract and Firebase project are established. The first milestone uses a fake Mac agent, Firebase Auth and Firestore emulators, an Android phone emulator, and a standalone Wear OS emulator.

- Firebase project: `watch-my-ai-doing`
- Android package: `com.oxiomsystems.wmaid`
- Cloud deployment: not enabled
- Google Play release: held until a developer account is available
- Live Claude and Codex adapters: deferred until the emulator showcase passes

## Product shape

```mermaid
flowchart TB
  A[Claude Code or Codex] --> M[Mac watcher]
  M --> R[WMAID relay]
  R --> W[Standalone Wear OS app]
  R --> P[Android phone app]
  W --> R
  P --> R
  R --> M
  P --> C[Provider mobile session]
```

The watch is a first-class network client. It does not require a live phone connection for its core notification and reply flow.

## Emulator-first definition

The first runnable demonstration must prove:

1. Fake pairing on both phone and watch.
2. A simulated Claude or Codex interruption reaches both clients.
3. A reply from the watch resolves the same event on the phone.
4. A long question hands off to a simulated provider-session screen.
5. Repeatable commands capture phone and round-watch screenshots.

See the [requirements-only plan](plans/2026-07-30-001-feat-wmaid-emulator-first-agent-notifications-plan.md) and [architecture](docs/architecture.md).

## Security

The repository must never contain:

- signing keys or passwords
- Firebase service-account credentials
- personal Claude or Codex transcripts
- production access tokens
- real prompt content captured without an explicit project policy

Firebase client configuration and release credentials are materialized from local configuration or private GitHub Actions secrets.

## License

Apache-2.0
