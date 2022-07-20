package net.id.incubus_core.task;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.WeakHashMap;

public class Tasks {

    private static final WeakHashMap<Identifier, AbstractTask> GAME_TASKS = new WeakHashMap<>();
    private static final WeakHashMap<UUID, WeakHashMap<Identifier, AbstractTask>> PLAYER_TASKS = new WeakHashMap<>();

    public static boolean store(AbstractTask task, @Nullable PlayerEntity associatedPlayer) {
        if(associatedPlayer == null) {
            if (!GAME_TASKS.containsKey(task.taskId) || !GAME_TASKS.get(task.taskId).isActive()) {
                GAME_TASKS.put(task.taskId, task);
                return true;
            }
        }
        else {
            var tasksSet = new WeakHashMap<Identifier, AbstractTask>();

            if(PLAYER_TASKS.containsKey(associatedPlayer.getUuid()))
                tasksSet = PLAYER_TASKS.get(associatedPlayer.getUuid());

            if(!tasksSet.containsKey(task.taskId) || !tasksSet.get(task.taskId).isActive()) {
                tasksSet.put(task.taskId, task);
                PLAYER_TASKS.put(associatedPlayer.getUuid(), tasksSet);
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static AbstractTask remove(Identifier taskId, @Nullable PlayerEntity associatedPlayer) {
        if(associatedPlayer == null)
            return GAME_TASKS.remove(taskId);
        else
            if(PLAYER_TASKS.containsKey(associatedPlayer.getUuid()))
                return PLAYER_TASKS.get(associatedPlayer.getUuid()).remove(taskId);
        return null;
    }

    @Nullable
    public static TaskInfo getInfo(Identifier taskId, @Nullable PlayerEntity associatedPlayer) {
        if(associatedPlayer == null)
            return GAME_TASKS.get(taskId);
        else
            if(PLAYER_TASKS.containsKey(associatedPlayer.getUuid()))
                return PLAYER_TASKS.get(associatedPlayer.getUuid()).get(taskId);
        return null;
    }
}
