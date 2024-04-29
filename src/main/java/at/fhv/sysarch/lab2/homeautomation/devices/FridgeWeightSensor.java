package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class FridgeWeightSensor extends AbstractBehavior<FridgeWeightSensor.FridgeWeightSensorCommand> {
    public interface FridgeWeightSensorCommand {}

    public static final class GetRemainingWeightCommand implements FridgeWeightSensorCommand {
        ActorRef<FridgeOrderProcessor.FridgeOrderProcessorCommand> orderProcessor;

        public GetRemainingWeightCommand(ActorRef<FridgeOrderProcessor.FridgeOrderProcessorCommand> orderProcessor) {
            this.orderProcessor = orderProcessor;
        }
    }

    public static final class WeightChangedCommand implements FridgeWeightSensorCommand {
        private final double weight;

        public WeightChangedCommand(double weight) {
            this.weight = weight;
        }
    }

    private final String groupId;
    private final String deviceId;
    private double maxWeight = 30;
    private double usedWeight;

    public static Behavior<FridgeWeightSensor.FridgeWeightSensorCommand> create(double usedWeight, String groupId, String deviceId) {
        return Behaviors.setup(context -> new FridgeWeightSensor(context, usedWeight, groupId, deviceId));
    }

    private FridgeWeightSensor(
            ActorContext<FridgeWeightSensor.FridgeWeightSensorCommand> context,
            double usedWeight,
            String groupId,
            String deviceId
    ) {
        super(context);
        this.usedWeight = usedWeight;
        this.groupId = groupId;
        this.deviceId = deviceId;
        getContext().getLog().info("FridgeWeightSensor started");
    }

    @Override
    public Receive<FridgeWeightSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetRemainingWeightCommand.class, this::onGetRemainingWeight)
                .onMessage(WeightChangedCommand.class, this::onWeightChanged)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<FridgeWeightSensorCommand> onGetRemainingWeight(GetRemainingWeightCommand getRemainingWeightCommand) {
        getRemainingWeightCommand.orderProcessor.tell(new FridgeOrderProcessor.GetRemainingWeightCommandAnswer(maxWeight - usedWeight));
        return this;
    }

    private Behavior<FridgeWeightSensorCommand> onWeightChanged(WeightChangedCommand weightChangedCommand) {
        usedWeight = weightChangedCommand.weight;
        return this;
    }

    private Behavior<FridgeWeightSensorCommand> onPostStop() {
        getContext().getLog().info("FridgeWeightSensor stopped");
        return this;
    }
}
