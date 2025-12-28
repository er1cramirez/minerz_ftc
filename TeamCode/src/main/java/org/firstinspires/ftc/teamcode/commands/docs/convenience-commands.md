---
description: Using SolversLib-provided Commands to Enhance Your Program
---

# Convenience Features

SolversLib offers convenience features to make your paradigm program more compact. The idea is that it improves the overall structure of your program. Some of these convenience commands are just that: for convenience. This does not mean they are exceptional. Plenty are simplified for minimal competitive use, such as the [`PurePursuitCommand`](../../pathing/pure-pursuit/#using-the-pure-pursuit-command).

## Framework Commands

Framework commands exist to decrease the amount of program needed for simplistic tasks, like updating a number. Rather than having the user create an entire command for one simplistic task (which when done for multiple menial tasks builds up and makes the structure fairly disheveled), the user can utilize framework commands.

### InstantCommand

`InstantCommand` is a versatile framework command that on initialization runs some task and then ends on that same iteration of the CommandScheduler's `run()` method. This is especially useful for button-triggered events.

```java
GamepadEx toolOp = new GamepadEx(gamepad2);

toolOp.getGamepadButton(GamepadKeys.Button.A)
    .whenPressed(new InstantCommand(() -> {
        // your implementation of run() here
    }));

/* AN EXAMPLE */

Motor intakeMotor = new Motor(hardwareMap, "intake");

toolOp.getGamepadButton(GamepadKeys.Button.RIGHT_BUMPER)
    .whileHeld(new InstantCommand(() -> {
        intakeMotor.set(0.75);
    }))
    .whenReleased(new InstantCommand(intakeMotor::stopMotor));
```

You should actually use subsystems here instead of the motor object. That way we can add the subsystem's requirements to the `InstantCommand`. Below is a proper example.

{% code title="Intake.java" %}
```java
/**
 * This is a pedagogical intake subsystem for
 * a universal intake consisting of one motor
 * that drives a belt that connects to a pulley
 * that drives a PVC tube.
 */
public class Intake extends SubsystemBase {
    private Motor m_intakeMotor;
    
    public Intake(Motor intakeMotor) {
        m_intakeMotor = intakeMotor;
    }
    
    public void run() {
        m_intakeMotor.set(0.75);
    }
    
    public void stop() {
        m_intakeMotor.stopMotor();
    }
}
```
{% endcode %}

And then we can set up our command bindings as such:

```java
/* in your opmode */

Motor intakeMotor = new Motor(hardwareMap, "intake");
Intake intake = new Intake(intakeMotor);

toolOp.getGamepadButton(GamepadKeys.Button.RIGHT_BUMPER)
    // the second parameter is a varargs of subsystems
    // to require
    .whileHeld(new InstantCommand(intake::run, intake))
    .whenReleased(new InstantCommand(intake::stop, intake));
```

This removes a lot of unnecessary clutter of commands since in a custom implementation the user would have to define a command for both running the intake and stopping it. With `InstantCommand`, the amount of code on the user-side is dramatically reduced.

### RunCommand

As opposed to an `InstantCommand`, a `RunCommand` runs a given method in its execute phase. This is useful for `PerpetualCommand`s, default commands, and simple commands like driving a robot.

```java
/* in your opmode */

Motor intakeMotor = new Motor(hardwareMap, "intake");
Intake intake = new Intake(intakeMotor);

toolOp.getGamepadButton(GamepadKeys.Button.RIGHT_BUMPER)
    // the second parameter is a varargs of subsystems
    // to require
    .whileHeld(new RunCommand(intake::run, intake))
    .whenReleased(new InstantCommand(intake::stop, intake));
    
// say we have a drive subsystem
// with a drive method
schedule(new RunCommand(driveSubsystem::drive, driveSubsystem));
```

### ConditionalCommand

`ConditionalCommand` has a wide variety of uses. `ConditionalCommand` takes two commands and runs one when supplied a value of true, and another when supplied a value of false.

One use is for making a toggle between commands without using the inactive or active states of the `toggleWhenPressed()` binding. Let's update our intake to have two more methods we can use for this toggling feature:

{% code title="Intake.java" %}
```java
/**
 * This is a pedagogical intake subsystem for
 * a universal intake consisting of one motor
 * that drives a belt that connects to a pulley
 * that drives a PVC tube.
 */
public class Intake extends SubsystemBase {
    private Motor m_intakeMotor;
    private boolean m_active;
    
    public Intake(Motor intakeMotor) {
        m_intakeMotor = intakeMotor;
        m_active = true;
    }
    
    // switch the toggle
    public void toggle() {
        m_active = !m_active;
    }
    
    // return the active state
    public boolean active() {
        return m_active;
    }
    
    public void run() {
        m_intakeMotor.set(0.75);
    }
    
    public void stop() {
        m_intakeMotor.stopMotor();
    }
}
```
{% endcode %}

We can then use a conditional command in a trigger binding to produce a toggling effect once pressed:

```java
/* in your opmode */

Motor intakeMotor = new Motor(hardwareMap, "intake");
Intake intake = new Intake(intakeMotor);

toolOp.getGamepadButton(GamepadKeys.Button.RIGHT_BUMPER)
    .whenPressed(new ConditionalCommand(
        new InstantCommand(intake::run, intake),
        new InstantCommand(intake::stop, intake),
        () -> {
            intake.toggle();
            return intake.active();
        }
    ));
```

An example of usage could be in Velocity Vortex, where the beacons were either red or blue. Using a color sensor, we can detect the color and then perform some action based on whether it was red or blue.

```java
// pseudocode for instantiating the command
ConditionalCommand pressBeacon = new ConditionalCommand(
    new InstantCommand(beaconPresser::pushRed, beaconPresser),
    new InstantCommand(beaconPresser::pushBlue, beaconPresser),
    () -> vision.output() == BeaconColor.RED
);

pressBeacon.schedule();    // schedule the command

/* This is also useful in sequential command groups */
```

As you can see, conditional commands are very useful for switching between states with a certain state. We will see later that we would want to use a `SelectCommand` when working with several states and not a simple command that switches between two.

### UninterruptibleCommand

Schedules a given command as uninterruptible. This command's paramater is single command, so multiple commands need to be put in a CommandGroup first. See [#schedulecommand](convenience-commands.md#schedulecommand "mention")for scheduling commands as interruptible.

```java
// With one command:
UninterruptibleCommand uninterruptibleCommand = new UninterruptibleCommand(
    // Command
);


// With multiple commands:
UninterruptibleCommand uninterruptibleCommand = new UninterruptibleCommand(
    new SequentialCommandGroup(
        // Command, 
        // Command
    )
);
```

### ScheduleCommand

Does exactly as the name suggests: schedules commands (all as interruptible). You can input a variable number of command arguments to schedule, and the command will schedule them on initialization. After this, the command will finish. This is useful for forking off of command groups. See [#uninterruptiblecommand](convenience-commands.md#uninterruptiblecommand "mention") for scheduling commands as interruptible.

So far we've been using the convenience commands we've learned in tandem and how they can be used together to produce more efficient paradigm utility. This is no exception for the `ScheduleCommand`. We can use a conditional command to schedule a desired command.

```java
SequentialCommandGroup auto = new SequentialCommandGroup(
    ...,
    new ConditionalCommand(
        new ScheduleCommand(
            // schedule commands
        ),
        new ScheduleCommand(
            // schedule commands
        ),
        ...    // boolean supplier
    ),
    ...
);
```

It is important to note that the schedule command will finish immediately. Which means any following commands in the sequential command group will be run. The point of the schedule command is to fork off from the command group so that it can be run separately by the scheduler.

### SelectCommand

The select command is similar to a conditional command but for several commands. This is especially useful for a state machine. Let's take a look at the ring stack from the 2020-2021 Ultimate Goal season.

```java
public enum Height {
    ZERO, ONE, FOUR
}

public Height height() {
    // some code to detect height of the starter stack
}

...

SelectCommand wobbleCommand = new SelectCommand(
    // the first parameter is a map of commands
    new HashMap<Object, Command>() {{
        put(Height.ZERO, new new PurePursuitCommand(...)));
        put(Height.ONE, new PurePursuitCommand(...)));
        put(Height.FOUR, new PurePursuitCommand(...)));
    }},
    // the selector
    this::height
);
```

A select command is finished when the selected command also finishes. An alternative use of the select command is to run a command given a supplier instead of a map. Below is a comparable version to the one above:

```java
public enum Height {
    ZERO, ONE, FOUR
}

public Height height() {
    // some code to detect height of the starter stack
}

// returns a command for the wobble goal action
public Command wobbleCommand() {
    Height rings = this.height();
    switch (rings) {
        case Height.ZERO:
            return ...;
        case Height.ONE:
            return ...;
        case Height.FOUR:
            return ...;
    }
}

...

SelectCommand wobbleCommand = new SelectCommand(
    // pass in a command supplier
    this::wobbleCommand
);
```

### PerpetualCommand

As opposed to an instant command, a perpetual command swallows a command and runs it perpetually i.e. it will continue to execute the command passed in as an as argument in the constructor and ignore that command's `isFinished` condition. It can only end if it is interrupted. This makes them useful for default commands, which are interrupted when another command requiring that subsystem is currently being run and is not scheduled again until that command ends.

Let's take a look back at the command bindings for when we learned `InstantCommand`, Instead of doing `whileHeld` and `whenReleased` binding, a more idiomatic method is to use a default command to stop the intake when the button is released instead (which cancels the command once the trigger/button is inactive, allowing the default command to be scheduled).

```java
/* in your opmode */

Motor intakeMotor = new Motor(hardwareMap, "intake");
Intake intake = new Intake(intakeMotor);

toolOp.getGamepadButton(GamepadKeys.Button.RIGHT_BUMPER)
    .whileHeld(new InstantCommand(intake::run, intake));

intake.setDefaultCommand(new PerpetualCommand(stopIntakeCommand));

// this isn't actually needed; really you'd do this:
intake.setDefaultCommand(new RunCommand(intake::stop, intake));
```

Note that a perpetual command adds all the requirements of the swallowed command.

### RepeatCommand

`RepeatCommand` composes (wraps) another `Command` and repeatedly restarts it each time it finishes, until one of the configured termination conditions occurs.

Compared to `PerpetualCommand` (which simply keeps executing the wrapped command while suppressing its natural completion), `RepeatCommand` lets the wrapped command complete, explicitly ends it, and immediately initializes it again for a fresh cycle. This is ideal for discrete, restartable “unit” actions that you want to chain, such as for autonomous TeleOp cycling. Although it does not extend `CommandGroupBase`, `RepeatCommand` is treated as a command group.

RepeatCommand has 3 different constructors as follows:

#### 1. Repeat until interrupted:

```java
new RepeatCommand(command);
```

Runs `command` forever (until another command preempts it or it is canceled).

#### 2. Repeat until condition:

```java
new RepeatCommand(command, () -> someCondition);
```

Effectively a repeat until loop, with the second parameter being a `BooleanSupplier` condition. In other words, it repeats `command` until `someCondition` is true.

3. Repeat an integer amount of times

```java
new RepeatCommand(command, repeatTimes);
```

Repeats the wrapped command for the number given in the second parameter. As such, `repeatTimes` must be >= 0.

### RetryCommand

`RetryCommand` is a command originating from [Marrow](https://skeleton-army.gitbook.io/marrow). Designed for SolversLib, it's been moved over to SolversLib natively for convenience. The documentation below for it is copied vertabrim from Marrow: \
\
`RetryCommand` is designed for building adaptable and reliable command sequences. It executes a command, and, if the specified condition is not met upon completion, automatically retries up to the specified amount of times.

This is especially useful for actions that may fail on the first attempt, such as vision-based alignment, object grabbing, or precise mechanism positioning.



To use `RetryCommand`, you need to provide the constructor with:

* **Command to run** – the initial action you want to execute.
* **(Optional) Alternative command to run on retries** – lets you customize the retry behavior per attempt (e.g., switching to a vision-assisted command if the initial attempt fails).
* **Success condition** – a boolean supplier that checks whether the action was successful. If this condition returns `false`, the command will be retried.
* **Maximum number of retries** – defines the maximum number of times the command can be retried.

The Constructors are as follows:

#### 1. Basic RetryCommand (Same Command on Repeat)

```java
RetryCommand(Command command, BooleanSupplier successCondition, int maxRetries)

// Example
new RetryCommand(
    new GrabCommand(claw),   // Command to run.
    () -> claw.isGrabbed(),  // If this condition is false
    5                        // retry up to 5 times.
)
```

#### 2. Advanced RetryCommand (Different Command on Repeat)

```java
RetryCommand(Command command, Command retryCommand, BooleanSupplier successCondition, int maxRetries)

// Example
new RetryCommand(
    new GrabCommand(claw),                  // Command to run initially.
    new DetectAndGrabCommand(claw, vision), // Command to run on each retry.
    () -> claw.isGrabbed(),                 // If this condition is false
    5                                       // retry up to 5 times.
)
```

This repeats a command that isn't the original that fails.



Here’s an example autonomous that uses `RetryCommand` together with SolversLib’s command system:

```java
@Autonomous
public class MyAuto extends CommandOpMode {
    private ClawSubsystem clawSubsystem;
    private VisionSubsystem visionSubsystem;

    @Override
    public void initialize() {
        clawSubsystem = new ClawSubsystem(hardwareMap);
        visionSubsystem = new VisionSubsystem(hardwareMap);

        schedule(
            new SequentialCommandGroup(
                // First, try to grab. If unsuccessful, retry grabbing up to 3 times.
                new RetryCommand(
                    new GrabCommand(clawSubsystem),
                    () -> clawSubsystem.isHoldingGameElement(),
                    3
                ),
                
                // ---- OR ----
                
                // First, try to grab. If unsuccessful, try to detect and grab using vision up to 3 times.
                new RetryCommand(
                    new GrabCommand(clawSubsystem),
                    new DetectAndGrabCommand(clawSubsystem, visionSubsystem), 
                    () -> clawSubsystem.isHoldingGameElement(),
                    3
                )
            )
        );
    }
}
```

{% hint style="warning" %}
This is by no means a functional autonomous program, and is purely used as an example.
{% endhint %}

Check out the [Marrow Retries page](https://skeleton-army.gitbook.io/marrow/concepts/beyond-the-basics/retries) for more info.

### WaitUntilCommand

A `WaitUntilCommand` is run until the boolean supplied returns true. This is useful for when you have forked off from a command group. Let's expand upon the example from the [`ScheduleCommand`](convenience-commands.md#schedulecommand) but with a single schedule command.

```java
SequentialCommandGroup auto = new SequentialCommandGroup(
    ...,
    // schedule the command that forks off from
    // the command group
    new ScheduleCommand(forkedCommand),
    // wait until that command is finished
    new WaitUntilCommand(forkedCommand::isFinished),
    ...    // more commands
);
```

The following commands in the auto command group are only run after that forked command is finished due to that `WaitUntilCommand`.

### StartEndCommand

The `StartEndCommand` is essentially an `InstantCommand` with a custom end function. It takes two `Runnable` parameters an optional varargs of subsystems to require. The first parameter is run on initialize and the second is run on end.

### DeferredCommand

Usually, when you schedule a command (like `new IntakeCommand(intake)`), all of its dependencies, parameters, and logic is fixed at construction time (when the command is instantiated, not run).

Sometimes, you don't know what command you need until later. For example:

* You want to choose the command based on **sensor data**
* You want to choose the command based on the **most recent state of a variable**

The `DeferredCommand` waits until the command is executed to decide which command to run. It takes a `command` and a `list` of required `subsystems` as input.

```java

class Door extends SubsystemBase {
	private boolean isOpen = false;
	public static int DOOR_DELAY = 500;
	...,
	
	public Command setOpen(boolean newState){
		if (isOpen == newState) { 
			return new InstantCommand(); 
		}

		isOpen = newState;
		return new WaitCommand(DOOR_DELAY);
	}
}

schedule(
	new DeferredCommand(door.setOpen(false), door);
)
```

In this example, since the initial value of `isOpen` is `false`, without the use of `DeferredCommand`, no matter what the current state of the door is, `door.setOpen(false)` would return an `InstantCommand`. By using `DeferredCommand`, you can make the command use the current state instead of the state when the command was instantiated.

Note that the example above is simple and can be handled by a [#conditionalcommand](convenience-commands.md#conditionalcommand "mention") more conveniently.

### FunctionalCommand

The last framework command we will discuss is the `FunctionalCommand`. It is useful for doing an inline definition of a complex command instead of creating a new command subclass. Generally, it is better to write a class for anything past a certain complexity.

The `FunctionalCommand` takes four inputs and a variable amount of subsystems for adding requirements. The four inputs determine the initialization, execution, and end actions, followed by the boolean supplier that determines if it is finished.

A good use of this is inside of a sequential command group. Let's use the previous command group and add a functional command.

```java
SequentialCommandGroup auto = new SequentialCommandGroup(
    ...,
    new FunctionalCommand(
        // init actions
        driveSubsystem::resetEncoders,
        // execute actions
        () -> {
            /* list of actions */
            // remember run() returns void
        },
        // end actions
        driveSubsystem::stop,
        // is finished supplier
        () -> {
            /* logic that returns a boolean */
        },
        driveSubsystem,
        ...    // any other required subsystems
    ),
    new ScheduleCommand(forkedCommand),
    new WaitUntilCommand(forkedCommand::isFinished),
    ...
);
```



## Command Decorators

Decorators are methods that allow you to make command bindings and logic without having to create new commands.

### withTimeout

Returns a `ParallelRaceGroup` with a specified timeout in milliseconds.

```java
schedule(
    fooCommand.withTimeout(1000) // ends after 1000 milliseconds
);
```

### interruptOn

Returns a `ParallelRaceGroup` that ends once a specified condition is met.

```java
schedule(
    fooCommand.interruptOn(() -> {
        /* BOOLEAN SUPPLIER */
        return ...;
    })
);
```

### whenFinished

Returns a `SequentialCommandGroup` that runs a given Runnable after the calling command finishes.

```java
schedule(
    fooCommand.whenFinished(() -> {
        /* RUNNABLE */
    })
);
```

### beforeStarting

Returns a `SequentialCommandGroup` that runs a given Runnable before the calling command is initialized.

```java
schedule(
    fooCommand.beforeStarting(() -> {
        /* RUNNABLE */
    })
);
```

### beforeStarting (overloaded)

An overloaded method of [beforeStarting](convenience-commands.md#beforestarting) that takes a Command as a parameter instead of a Runnable

```java
schedule(
    fooCommand.beforeStarting(() -> {
        /* COMMAND */
    })
);
```

### andThen

Returns a `SequentialCommandGroup` that runs all the given commands in sequence after the calling command finishes.

```java
schedule(
    fooCommand.andThen(
        barCommand, bazCommand, ...
    )
);
```

### deadlineWith

Returns a `ParallelDeadlineGroup` with the calling command as the deadline to run in parallel with the given commands.

```java
schedule(
    // ends when fooCommand finishes
    fooCommand.deadlineWith(
        barCommand, bazCommand, ...
    )
);
```

### alongWith

Returns a `ParallelCommandGroup` that runs the given commands in parallel with the calling command.

```java
schedule(
    fooCommand.alongWith(
        barCommand, bazCommand, ...
    )
);
```

### raceWith

Returns a `ParallelRaceGroup` that runs all the commands in parallel until one of them finishes.

```java
schedule(
    // runs until one of the commands finishes
    fooCommand.raceWith(
        barCommand, bazCommand, ...
    )
);
```

### perpetually

Swallows the command into a `PerpetualCommand` and returns it.

```java
// returns a perpetual command
PerpetualCommand perpetual = fooCommand.perpetually();
```

### asProxy

Swallows the command into a `ProxyScheduleCommand` and returns it. This is similar to a `ScheduleCommand` except it ends when all the commands that it scheduled are finished rather than immediately.

```java
// reurns a proxy schedule command
ProxyScheduleCommand proxySchedule = fooCommand.asProxy();
```
