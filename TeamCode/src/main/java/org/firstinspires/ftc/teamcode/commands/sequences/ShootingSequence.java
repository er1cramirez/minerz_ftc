package org.firstinspires.ftc.teamcode.commands.sequences;

import com.seattlesolvers.solverslib.command.CommandBase;
import com.seattlesolvers.solverslib.command.CommandScheduler;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.WaitCommand;
import com.seattlesolvers.solverslib.command.WaitUntilCommand;
import com.seattlesolvers.solverslib.command.InstantCommand;

import org.firstinspires.ftc.teamcode.commands.ejector.EjectCycleCommand;
import org.firstinspires.ftc.teamcode.commands.spindexer.SpindexerMoveCommand;
import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.FlywheelSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem.SlotState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Configurable sequence to shoot balls based on a specified strategy.
 * 
 * Supports:
 * - Shooting Orders: GREEN_FIRST, GREEN_MIDDLE, GREEN_LAST, FASTEST
 * - Dynamic Delays: Adjusts wait time based on servo travel distance.
 */
public class ShootingSequence extends SequentialCommandGroup {

    public enum ShootingStrategy {
        GREEN_FIRST,
        GREEN_MIDDLE,
        GREEN_LAST,
        FASTEST // Optimized path 1 -> 0 -> 2
    }

    private static final long SHORT_WAIT_MS = 350;
    private static final long LONG_WAIT_MS = 950;
    private static final double LONG_TRAVEL_THRESHOLD = 0.35; // ~105 degrees

    public ShootingSequence(SpindexerSubsystem spindexer, FlywheelSubsystem flywheel, EjectorSubsystem ejector, ShootingStrategy strategy) {
        // Requirements
        addRequirements(spindexer, ejector);

        // 1. Determine Order based on CURRENT state (at instantiation)
        List<Integer> targetSlots = determineShootingOrder(spindexer, strategy);
        
        if (targetSlots.isEmpty()) {
            return; // No commands added, finishes immediately
        }
        
        int currentSlot = spindexer.getCurrentSlotIndex();
        
        for (int slotIndex : targetSlots) {
            // Calculate dynamic wait based on travel from 'current' to 'next'
            long waitTime = calculateWaitTime(currentSlot, slotIndex);
            
            addCommands(
                // Move to Outtake
                new SpindexerMoveCommand(spindexer, slotIndex, SpindexerMoveCommand.Position.OUTTAKE),
                
                // Wait for Servo Movement
                new WaitCommand(waitTime),

                // Wait for Flywheel Stability
                // TODO: UNCOMMENT THIS LINE AFTER VERIFYING SPINDEXER MOVEMENTS!
                // new WaitUntilCommand(flywheel::isReadyToShoot),
            
                // Eject
                new EjectCycleCommand(ejector),
             
                // Mark slot as empty (Logical update)
                new InstantCommand(() -> spindexer.clearSlot(slotIndex))
            );
             
             currentSlot = slotIndex;
        }
    }
    
    private List<Integer> determineShootingOrder(SpindexerSubsystem spindexer, ShootingStrategy strategy) {
        List<Integer> slots = new ArrayList<>();
        
        // Populate valid slots
        for(int i=0; i<3; i++) {
             if(spindexer.getSlotState(i) != SlotState.EMPTY) {
                 slots.add(i);
             }
        }
        
        if (slots.isEmpty()) return slots;

        switch (strategy) {
            case FASTEST:
                // Hardcoded preference: 1 -> 0 -> 2
                // Sort based on index preference
                slots.sort(Comparator.comparingInt(this::getFastestPriority));
                break;
                
            case GREEN_FIRST:
                slots.sort((a, b) -> {
                    boolean aGreen = spindexer.getSlotState(a) == SlotState.GREEN;
                    boolean bGreen = spindexer.getSlotState(b) == SlotState.GREEN;
                    return Boolean.compare(!aGreen, !bGreen); // Green (true) comes first
                });
                break;
                
            case GREEN_LAST:
                slots.sort((a, b) -> {
                    boolean aGreen = spindexer.getSlotState(a) == SlotState.GREEN;
                    boolean bGreen = spindexer.getSlotState(b) == SlotState.GREEN;
                    return Boolean.compare(aGreen, bGreen); // Green (true) comes last
                });
                break;
                
             case GREEN_MIDDLE:
                 List<Integer> green = new ArrayList<>();
                 List<Integer> others = new ArrayList<>();
                 for(int s : slots) {
                     if(spindexer.getSlotState(s) == SlotState.GREEN) green.add(s);
                     else others.add(s);
                 }
                 
                 slots.clear();
                 if (!others.isEmpty()) slots.add(others.remove(0));
                 slots.addAll(green);
                 slots.addAll(others);
                 break;
        }
        return slots;
    }
    
    private int getFastestPriority(int slotIndex) {
        // Lower is better
        // 1 -> 0 -> 2
        if (slotIndex == 1) return 0;
        if (slotIndex == 0) return 1;
        if (slotIndex == 2) return 2;
        return 3;
    }

    private long calculateWaitTime(int fromSlot, int toSlot) {
        if (fromSlot == toSlot) return 100; // Same slot adjustment
        
        // Positions (0.0-1.0)
        // Slot 0: 180 (0.6)
        // Slot 1: 300 (1.0)
        // Slot 2: 60  (0.2)
        
        double posFrom = getOuttakePos(fromSlot);
        double posTo = getOuttakePos(toSlot);
        
        double dist = Math.abs(posTo - posFrom);
        
        if (dist > LONG_TRAVEL_THRESHOLD) return LONG_WAIT_MS;
        return SHORT_WAIT_MS;
    }
    
    // Helper to avoid circular dependency on Constants (or just hardcode logic mapping)
    private double getOuttakePos(int slot) {
        if (slot == 0) return 0.6;
        if (slot == 1) return 1.0;
        if (slot == 2) return 0.2;
        return 0.0;
    }
}
