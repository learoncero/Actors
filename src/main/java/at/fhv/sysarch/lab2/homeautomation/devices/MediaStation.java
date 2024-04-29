package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class MediaStation extends AbstractBehavior<MediaStation.MediaStationCommand> {
    public interface MediaStationCommand {}

    public static final class PlayMediaCommand implements MediaStationCommand {
    }

    public static final class StopMediaCommand implements MediaStationCommand {
    }

    private final String groupId;
    private final String deviceId;
    private boolean isMediaPlaying = false;
    private final ActorRef<Blinds.BlindsCommand> blinds;

    public static Behavior<MediaStationCommand> create(
            ActorRef<Blinds.BlindsCommand> blinds,
            String groupId,
            String deviceId) {
        return Behaviors.setup(context -> new MediaStation(context, blinds, groupId, deviceId));
    }

    private MediaStation(
            ActorContext<MediaStation.MediaStationCommand> context,
            ActorRef<Blinds.BlindsCommand> blinds,
            String groupId,
            String deviceId
    ) {
        super(context);
        this.blinds = blinds;
        this.groupId = groupId;
        this.deviceId = deviceId;
        getContext().getLog().info("MediaStation started");
    }

    @Override
    public Receive<MediaStationCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(PlayMediaCommand.class, this::onPlayMedia)
                .onMessage(StopMediaCommand.class, this::onStopMedia)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<MediaStationCommand> onPlayMedia(PlayMediaCommand r) {
        if (!isMediaPlaying) {
            getContext().getLog().info("MediaStation starting media");
            isMediaPlaying = true;
            this.blinds.tell(new Blinds.MediaStationChangeStateCommand(true));
        } else {
            getContext().getLog().info("MediaStation is already playing media");
        }
        return this;
    }

    private Behavior<MediaStationCommand> onStopMedia(StopMediaCommand r) {
        if (isMediaPlaying) {
            getContext().getLog().info("MediaStation stopping media");
            isMediaPlaying = false;
            this.blinds.tell(new Blinds.MediaStationChangeStateCommand(false));
        } else {
            getContext().getLog().info("MediaStation can't stop media because it is already stopped");
        }
        return this;
    }

    private MediaStation onPostStop() {
        getContext().getLog().info("MediaStation actor {}-{} stopped", groupId, deviceId);
        return this;
    }
}
