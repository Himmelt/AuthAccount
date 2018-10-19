package org.soraworld.account.serializer;

import org.soraworld.hocon.exception.NotMatchException;
import org.soraworld.hocon.exception.NullNodeException;
import org.soraworld.hocon.node.Node;
import org.soraworld.hocon.node.NodeBase;
import org.soraworld.hocon.node.Options;
import org.soraworld.hocon.serializer.TypeSerializer;

import java.lang.reflect.Type;
import java.util.regex.Pattern;

public class PatternSerializer implements TypeSerializer<Pattern> {
    public Pattern deserialize(Type type, Node node) throws NullNodeException, NotMatchException {
        if (node == null) throw new NullNodeException();
        if (node instanceof NodeBase) {
            return Pattern.compile(((NodeBase) node).getString());
        } else throw new NotMatchException("Pattern type need NodeBase");
    }

    public Node serialize(Type type, Pattern value, Options options) {
        return new NodeBase(options, value.pattern(), false);
    }

    public Type getRegType() {
        return Pattern.class;
    }
}
