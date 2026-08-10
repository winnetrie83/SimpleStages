# Simple Stages 1.3.0 release cleanup

Apply this patch over the tested 1.3.0-dev3.8 source.

1. Copy the patch contents into the project root and overwrite matching files.
2. Delete every path listed in `REMOVE-OLD-FILES.txt` if it still exists. You can do this automatically with `./DELETE-OBSOLETE.ps1`.
3. Optionally run `./VERIFY-CLEANUP.ps1` from PowerShell in the project root.
4. Run your normal Gradle build.
5. Launch a client and, ideally, a dedicated server once before pushing/releasing.

This cleanup intentionally does not change gameplay behavior from the tested dev3.8 design. It removes obsolete development paths, removes temporary compatibility code/debug output, updates documentation/metadata, and sets the release version to 1.3.0.

Network protocol remains `stage_editor_4`.
Stage storage schema remains `1`.
