package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.domain.BlindsState;

public class Blinds extends AbstractBehavior<Blinds.BlindsCommand> {
    public interface BlindsCommand {}

    public static final class OpenBlindsCommand implements BlindsCommand {
    }

    public static final class CloseBlindsCommand implements BlindsCommand {
    }

    public static final class MediaStationChangeStateCommand implements BlindsCommand {
        private final boolean isMediaPlaying;

        public MediaStationChangeStateCommand(boolean isMediaPlaying) {
            this.isMediaPlaying = isMediaPlaying;
        }
    }

    private final String groupId;
    private final String deviceId;
    private BlindsState state = BlindsState.OPEN;
    private boolean isMediaPlaying = false;

    public static Behavior<Blinds.BlindsCommand> create(String groupId, String deviceId) {
        return Behaviors.setup(context -> new Blinds(context, groupId, deviceId));
    }

    private Blinds(ActorContext<Blinds.BlindsCommand> context, String groupId, String deviceId) {
        super(context);
        this.groupId = groupId;
        this.deviceId = deviceId;
        getContext().getLog().info("Blinds started");
    }

    @Override
    public Receive<BlindsCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(OpenBlindsCommand.class, this::onOpenBlinds)
                .onMessage(CloseBlindsCommand.class, this::onCloseBlinds)
                .onMessage(MediaStationChangeStateCommand.class, this::onMediaStationChangeState)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<BlindsCommand> onOpenBlinds(OpenBlindsCommand r) {
        if(this.isMediaPlaying && this.state == BlindsState.CLOSED) {
            getContext().getLog().info("Media is playing so blinds will stay closed");
        }else if (this.state == BlindsState.CLOSED) {
            getContext().getLog().info("Opening blinds");
            this.state = BlindsState.OPEN;
        }

        return this;
    }

    private Behavior<BlindsCommand> onCloseBlinds(CloseBlindsCommand r) {
        if (this.state == BlindsState.OPEN) {
            getContext().getLog().info("Closing blinds");
            this.state = BlindsState.CLOSED;
        }

        return this;
    }

    private Behavior<BlindsCommand> onMediaStationChangeState(MediaStationChangeStateCommand mediaStationChangeStateCommand) {

        this.isMediaPlaying = mediaStationChangeStateCommand.isMediaPlaying;

        if (this.isMediaPlaying && this.state == BlindsState.OPEN) {
            getContext().getLog().info("Closing blinds because media is playing");
            this.state = BlindsState.CLOSED;
        }

        return this;
    }

    private Blinds onPostStop() {
        getContext().getLog().info("Blinds actor {}-{} stopped", groupId, deviceId);
        return this;
    }
}
