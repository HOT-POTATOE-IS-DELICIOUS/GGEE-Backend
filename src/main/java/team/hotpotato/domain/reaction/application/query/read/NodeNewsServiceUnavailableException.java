package team.hotpotato.domain.reaction.application.query.read;

public class NodeNewsServiceUnavailableException extends RuntimeException {
    public static final NodeNewsServiceUnavailableException EXCEPTION = new NodeNewsServiceUnavailableException();

    private NodeNewsServiceUnavailableException() {
        super("News service is unavailable");
    }
}
