# Widget Stack 1.11

## User-visible changes

- Add to home has been removed from the app's stack list. Use the launcher's widget picker to place saved stacks.
- Delete confirms and deletes the saved definition even if placed. All its placement links and selections are removed in one preference commit. The launcher-owned slots remain, displaying a prompt to choose another saved stack. Only child bindings not referenced by remaining definitions are released. Cancel preserves everything.
- Home-screen launcher Edit now opens the current stack editor. Use a different saved stack explicitly opens the chooser. Initial placement still opens the chooser. Completing or cancelling reconfiguration does not reassign the same stack or reset that placement's selected widget.
- Opening the editor or choosing Refresh widgets cancels stale contraction state, expands and republishes controls, and keeps the selected widget. The configured contraction delay then applies normally.
- A contracted corner visual keeps its configured size, centred inside a corner tap target of at least 48 dp. Both renderer and preview use this target. Only that small corner region intercepts provider content. The preview's full touch shield exists only in StackPreview, never in published RemoteViews.

## Investigation and limitations

The user reported that reselecting the same saved stack from launcher Edit made controls work again, and clarified that the widget's own settings button already opened the proper editor. This release addresses the incorrect launcher route and supplies the reset/publication step without reassignment.

A separate reproducible geometry defect put the default 8 dp contracted square at the extreme widget corner. An entire 8 by 8 square there lies outside a 28 dp rounded outline. Android launchers can automatically crop widget corners. The corner is now inset and its tap target enlarged. This establishes a real rendering defect; it does not prove that clipping was the sole cause on the user's device.

Android references:
- https://developer.android.com/about/versions/12/features/widgets
- https://developer.android.com/develop/ui/compose/glance/create-app-widget
- https://android.googlesource.com/platform/frameworks/base/+/android16-release/core/res/res/values/dimens.xml

## Verification

Eight new regression executions failed against 1.10 before the implementation changes. The final suite adds sixteen executions across API 31 and 35 for launcher edit/switch, corner geometry under 28 dp rounding, placed-stack deletion, cancellation, multiple-copy cleanup, preserving an unrelated definition, large configured corner sizes, and selection-preserving recovery through Edit and Refresh. Existing navigation/preview/animation tests remain in the full suite. Corner touch-dispatch checks tap outside the visible square but inside its larger target, verify EXPAND delivery, and verify provider clicks on the remaining edge.

The before-fix XML and final test/lint/build evidence are included in evidence-1.11. Framework tests use Robolectric and are not physical-launcher tests. No emulator or physical-device execution is claimed for 1.11. The earlier software-emulator infrastructure failure is documented in 1.10. Retest actual corner expansion and repeated navigation on the user's launcher.

Install as an update, then open Widget Stack once to republish layouts for existing copies. The signing key is unchanged and saved widget bindings are retained.

Final result: 179 test executions, zero failures/errors; lint passes. Instrumentation and fixture APKs compile.
