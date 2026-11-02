# Tasker control (1.33)

Enable **Enable Tasker control** in Widget Stack. This allows other apps to select positions through the public broadcast endpoint; it is off by default. **Tasker setup and instance IDs** lists the saved stack IDs and individual placement IDs, including placements hosted by Canvas.

To identify a particular placement, open its configuration button: its instance ID appears under the stack name.

In Tasker create a task with **System → Send Intent**:

- Action: `com.kevin.widgetstack.SET_POSITION`
- Package: `com.kevin.widgetstack`
- Class: `com.kevin.widgetstack.TaskerReceiver`
- Target: **Broadcast Receiver**
- Extra: `position:2`
- Extra: `instance_id:123` (replace 123 with the desired placement ID)

For all existing copies of a saved stack, replace the instance extra with `stack_id:-2` using the ID shown in the app. Use exactly one target. Positions start at 1 in the configured order. String and integer extras are accepted. Invalid commands do nothing. Saved defaults and other copies are unchanged by an individual selection. Reordering changes position meanings; removing/re-adding a placement changes its instance ID. Paused stacks save the requested selection and display it when resumed.

This selects a widget within an assigned stack; it does not reassign a placement to another saved stack. No Tasker plug-in or root is needed. Native Tasker/Xiaomi device verification remains required.
