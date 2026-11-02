# Tasker control (1.34)

Enable **Enable Tasker control** in Widget Stack. This allows other apps to select positions through the public broadcast endpoint; it is off by default. **Tasker setup and instance IDs** lists the saved stack IDs and individual placement IDs, including placements hosted by Canvas.

To identify a particular placement, open its configuration button: its instance ID appears under the stack name.

## Action plug-in (recommended)

In Tasker add **Plugin → Widget Stack**, open its configuration, choose the named stack (all copies) or a single instance, then choose the widget position and tap **Save action**. Enable Tasker control in this editor or in Widget Stack. Tasker displays a description of the selected target and position. Reopening the action restores its configuration. Cancel/Back returns without changing the action. Empty or removed targets require choosing an available stack.

No separate plug-in app is required. Widget Stack implements the standard Locale/Tasker EDIT_SETTING/FIRE_SETTING protocol. Configuration is stored in Tasker as primitive values; no calendar data is shared. It selects a position within a stack, rather than changing which saved stack is assigned to the placement.

## Manual Send Intent (still supported)

In Tasker create a task with **System → Send Intent**:

- Action: `com.kevin.widgetstack.SET_POSITION`
- Package: `com.kevin.widgetstack`
- Class: `com.kevin.widgetstack.TaskerReceiver`
- Target: **Broadcast Receiver**
- Extra: `position:2`
- Extra: `instance_id:123` (replace 123 with the desired placement ID)

For all existing copies of a saved stack, replace the instance extra with `stack_id:-2` using the ID shown in the app. Use exactly one target. Positions start at 1 in the configured order. String and integer extras are accepted. Invalid commands do nothing. Saved defaults and other copies are unchanged by an individual selection. Reordering changes position meanings; removing/re-adding a placement changes its instance ID. Paused stacks save the requested selection and display it when resumed.

This selects a widget within an assigned stack; it does not reassign a placement to another saved stack. No root is needed. Native Tasker/Xiaomi device verification remains required.
