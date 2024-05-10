package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.domain.Product;

import java.util.Optional;

public class FridgeOrderProcessor extends AbstractBehavior<FridgeOrderProcessor.FridgeOrderProcessorCommand> {
    public interface FridgeOrderProcessorCommand {}

    public static final class GetRemainingSpaceCommandAnswer implements FridgeOrderProcessorCommand {
        private final int remainingSpace;

        public GetRemainingSpaceCommandAnswer(int remainingSpace) {
            this.remainingSpace = remainingSpace;
        }
    }

    public static final class GetRemainingWeightCommandAnswer implements FridgeOrderProcessorCommand {
        private final double remainingWeight;

        public GetRemainingWeightCommandAnswer(double remainingWeight) {
            this.remainingWeight = remainingWeight;
        }
    }

    private final String groupId;
    private final String deviceId;
    private final ActorRef<Fridge.FridgeCommand> fridge;
    private final ActorRef<FridgeSpaceSensor.FridgeSpaceSensorCommand> fridgeSpaceSensor;
    private final ActorRef<FridgeWeightSensor.FridgeWeightSensorCommand> fridgeWeightSensor;
    private final Product product;
    private int remainingSpace;
    private double remainingWeight;

    public static Behavior<FridgeOrderProcessorCommand> create(
            ActorRef<Fridge.FridgeCommand> fridge,
            ActorRef<FridgeSpaceSensor.FridgeSpaceSensorCommand> fridgeSpaceSensor,
            ActorRef<FridgeWeightSensor.FridgeWeightSensorCommand> fridgeWeightSensor,
            Product product,
            String groupId,
            String deviceId
    ) {
        return Behaviors.setup(context -> new FridgeOrderProcessor(
                context,
                fridge,
                fridgeSpaceSensor,
                fridgeWeightSensor,
                product,
                groupId,
                deviceId
        ));
    }

    private FridgeOrderProcessor(
            ActorContext<FridgeOrderProcessorCommand> context,
            ActorRef<Fridge.FridgeCommand> fridge,
            ActorRef<FridgeSpaceSensor.FridgeSpaceSensorCommand> fridgeSpaceSensor,
            ActorRef<FridgeWeightSensor.FridgeWeightSensorCommand> fridgeWeightSensor,
            Product product,
            String groupId,
            String deviceId
    ) {
        super(context);
        this.fridge = fridge;
        this.fridgeSpaceSensor = fridgeSpaceSensor;
        this.fridgeWeightSensor = fridgeWeightSensor;
        this.product = product;
        this.groupId = groupId;
        this.deviceId = deviceId;

        fridgeSpaceSensor.tell(new FridgeSpaceSensor.GetRemainingSpaceCommand(getContext().getSelf()));
        fridgeWeightSensor.tell(new FridgeWeightSensor.GetRemainingWeightCommand(getContext().getSelf()));

        getContext().getLog().info("FridgeOrderProcessor started");
    }

    @Override
    public Receive<FridgeOrderProcessorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetRemainingSpaceCommandAnswer.class, this::onGetRemainingSpace)
                .onMessage(GetRemainingWeightCommandAnswer.class, this::onGetRemainingWeight)
                .build();
    }

    private Behavior<FridgeOrderProcessorCommand> onGetRemainingSpace(GetRemainingSpaceCommandAnswer getRemainingSpaceCommandAnswer) {
        this.remainingSpace = getRemainingSpaceCommandAnswer.remainingSpace;
        return processOrder();
    }

    private Behavior<FridgeOrderProcessorCommand> onGetRemainingWeight(GetRemainingWeightCommandAnswer getRemainingWeightCommandAnswer) {
        this.remainingWeight = getRemainingWeightCommandAnswer.remainingWeight;
        return processOrder();
    }

    private Behavior<FridgeOrderProcessorCommand> processOrder() {
        if (remainingSpace >= product.getSpace() && remainingWeight >= product.getWeight()) {
            fridge.tell(new Fridge.AddProductCommand(Optional.of(product)));
        } else {
            getContext().getLog().info("Not enough space or weight to order product {} with space {} and weight {}", product.getName(), product.getSpace(), product.getWeight());
        }

        return this;
    }

}
