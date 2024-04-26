package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.ActorContext;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.Receive;

public class Blind extends AbstractBehavior<Blind.BlindCommand> {
    public interface BlindCommand {}

    public static final class BlindUp implements BlindCommand {
    }

    public static final class BlindDown implements BlindCommand {
    }

    public static final class BlindStop implements BlindCommand {
    }

    public Blind(ActorContext<Blind.BlindCommand> context, String groupId, String deviceId) {
        super(context);
    }

    @Override
    public Receive<BlindCommand> createReceive() {
        return null;
    }
}
