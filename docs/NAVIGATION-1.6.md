# Navigation changes in 1.6

Open Edit stack, then Customize navigation bar. Appearance contains visibility, position, expanded thickness and opacity. Behaviour contains buttons-only mode, overlap and Auto-contract after 2000 ms. Animation contains the widget slide option. All sections use one Apply/Cancel draft.

Enable auto-contract to reveal the contracted-size slider. The minimum is 4 dp and the maximum is one dp below the expanded thickness. Tap the contracted strip to expand it. The first tap expands only; arrows and dots are available afterwards. Each navigation tap restarts the two-second delay. Ordinary provider refreshes and touches inside the widget do not restart it. With overlap off, changing bar thickness changes the space available to the widget; overlap avoids that size change.

The timer runs in the existing host process. As with widget updates, device sleep, process termination or OEM battery policy can delay its visible effect. Restart begins contracted. The feature defaults off and settings are independent for each stack. No repeating alarm or resize animation is introduced.

The preview displays the selected widget's actual cached content through the production renderer at its current home-screen content dimensions, scaled to fit. Its bar is local preview navigation; controls inside the widget perform real app actions. Provider updates refresh attached previews. Waiting content is labelled explicitly. Stock-launcher padding, rounded corners, colour treatment and timing can differ, so this is not a claim of pixel-identical launcher rendering.
