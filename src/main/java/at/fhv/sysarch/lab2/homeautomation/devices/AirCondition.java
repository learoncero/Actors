package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import java.util.Optional;

public class AirCondition extends AbstractBehavior<AirCondition.AirConditionCommand> {
    public interface AirConditionCommand {}

    public static final class PowerAirConditionCommand implements AirConditionCommand {
        final Optional<Boolean> value;

        public PowerAirConditionCommand(Optional<Boolean> value) {
            this.value = value;
        }
    }

    public static final class EnrichedTemperature implements AirConditionCommand {
        Optional<Double> value;
        Optional<String> unit;

        public EnrichedTemperature(Optional<Double> value, Optional<String> unit) {
            this.value = value;
            this.unit = unit;
        }
    }

    private final String groupId;
    private final String deviceId;
    private boolean active = false;
    private boolean poweredOn = true;

    public static Behavior<AirConditionCommand> create(String groupId, String deviceId) {
        return Behaviors.setup(context -> new AirCondition(context, groupId, deviceId));
    }

    private AirCondition(ActorContext<AirConditionCommand> context, String groupId, String deviceId) {
        super(context);
        this.groupId = groupId;
        this.deviceId = deviceId;
        getContext().getLog().info("AirCondition started");
    }

    @Override
    public Receive<AirConditionCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(EnrichedTemperature.class, this::onReadTemperature)
                .onMessage(PowerAirConditionCommand.class, this::onPowerAirConditionOff)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<AirConditionCommand> onReadTemperature(EnrichedTemperature r) {
        getContext().getLog().info("AirCondition reading {}", r.value.get());

        if(r.value.get() >= 20 && this.active == false && this.poweredOn == true) {
            getContext().getLog().info("AirCondition activated");
            this.active = true;
        }
        else if (r.value.get() < 20 && this.active == true && this.poweredOn == true) {
            getContext().getLog().info("AirCondition deactivated");
            this.active =  false;
        }

        return Behaviors.same();
    }

    private Behavior<AirConditionCommand> onPowerAirConditionOff(PowerAirConditionCommand r) {
        getContext().getLog().info("Turning AirCondition off");

        if(r.value.get() == false) {
            return this.powerOff();
        }
        return this;
    }

    private Behavior<AirConditionCommand> onPowerAirConditionOn(PowerAirConditionCommand r) {
        getContext().getLog().info("Turning AirCondition on");

        if(r.value.get() == true) {
            return Behaviors.receive(AirConditionCommand.class)
                    .onMessage(EnrichedTemperature.class, this::onReadTemperature)
                    .onMessage(PowerAirConditionCommand.class, this::onPowerAirConditionOff)
                    .onSignal(PostStop.class, signal -> onPostStop())
                    .build();
        }
        return this;
    }

    private Behavior<AirConditionCommand> powerOff() {
        this.poweredOn = false;
        return Behaviors.receive(AirConditionCommand.class)
                .onMessage(PowerAirConditionCommand.class, this::onPowerAirConditionOn)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private AirCondition onPostStop() {
        getContext().getLog().info("TemperatureSensor actor {}-{} stopped", groupId, deviceId);
        return this;
    }
}
