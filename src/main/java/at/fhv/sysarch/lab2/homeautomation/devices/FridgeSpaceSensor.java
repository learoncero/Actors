package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class FridgeSpaceSensor extends AbstractBehavior<FridgeSpaceSensor.FridgeSpaceSensorCommand> {
    public interface FridgeSpaceSensorCommand {}

    public static final class GetRemainingSpaceCommand implements FridgeSpaceSensorCommand {
        ActorRef<FridgeOrderProcessor.FridgeOrderProcessorCommand> orderProcessor;

        public GetRemainingSpaceCommand(ActorRef<FridgeOrderProcessor.FridgeOrderProcessorCommand> orderProcessor) {
            this.orderProcessor = orderProcessor;
        }
    }

    public static final class SpaceChangedCommand implements FridgeSpaceSensorCommand {
        private final int space;

        public SpaceChangedCommand(int space) {
            this.space = space;
        }
    }

    private final String groupId;
    private final String deviceId;
    private final int maxSpace = 10;
    private int usedSpace;

    public static Behavior<FridgeSpaceSensorCommand> create(int usedSpace, String groupId, String deviceId) {
        return Behaviors.setup(context -> new FridgeSpaceSensor(context, usedSpace, groupId, deviceId));
    }

    public FridgeSpaceSensor(
            ActorContext<FridgeSpaceSensor.FridgeSpaceSensorCommand> context,
            int usedSpace,
            String groupId,
            String deviceId
    ) {
        super(context);
        this.usedSpace = usedSpace;
        this.groupId = groupId;
        this.deviceId = deviceId;
        getContext().getLog().info("FridgeSpaceSensor started");
    }

    @Override
    public Receive<FridgeSpaceSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetRemainingSpaceCommand.class, this::onGetRemainingSpace)
                .onMessage(SpaceChangedCommand.class, this::onSpaceChanged)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<FridgeSpaceSensorCommand> onGetRemainingSpace(GetRemainingSpaceCommand getRemainingSpaceCommand) {
        getRemainingSpaceCommand.orderProcessor.tell(new FridgeOrderProcessor.GetRemainingSpaceCommandAnswer(maxSpace - usedSpace));
        return this;
    }

    private Behavior<FridgeSpaceSensorCommand> onSpaceChanged(SpaceChangedCommand spaceChangedCommand) {
        usedSpace = spaceChangedCommand.space;
        getContext().getLog().info("FridgeSpaceSensor space changed to {}", spaceChangedCommand.space);
        return this;
    }

    private FridgeSpaceSensor onPostStop() {
        getContext().getLog().info("FridgeSpaceSensor actor {} {} stopped", groupId, deviceId);
        return this;
    }
}
