package aiproject.agent;

import aiproject.game.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;

public class CollaborativeAgent extends AbstractAgent {

    private LinkedList<Point> pointList = new LinkedList<>();
    private ArrayList<Point> ourTargets = new ArrayList<>();
    private HashSet<Entity> foundTargets = new HashSet<>();
    private Point target = null;

    public CollaborativeAgent(int id) {
        super(id);
    }

    @Override
    public void init(Scenario scenario, int gameWidth, int gameHeight, int radarRange, int numTargets, int numAgents) {
        super.init(scenario, gameWidth, gameHeight, radarRange, numTargets, numAgents);
        for (int x = radarRange; x + radarRange <= gameWidth; x += radarRange) {
            for (int y = radarRange; y + radarRange <= gameHeight; y += radarRange) {
                pointList.add(new Point(x, y));
            }
        }
        pointList.add(new Point(0, 0));
        pointList.add(new Point(gameWidth - 1, gameHeight - 1));
        pointList.add(new Point(gameWidth - 1, 0));
        pointList.add(new Point(0, gameHeight - 1));
    }

    @Override
    public Move takeTurn(AgentContext ctx) {
        sendTargetPositions(ctx);
        sendPosition(ctx);
        readMessages(ctx);

        if (!ourTargets.isEmpty()) {
            target = getClosestTarget(ctx);
        }

        Point us = new Point(ctx.getX(), ctx.getY());
        if (target == null && !pointList.isEmpty()) {
            pointList.sort(Comparator.comparingDouble(a -> a.distance(us)));
            target = pointList.getFirst();
        }

        Move move = navigate(ctx, target);

        if (move == Move.NO_MOVE) {
            if (pointList.contains(target))
                pointList.removeFirst();
            if (ourTargets.contains(target))
                ourTargets.remove(target);
            target = null;
        }

        return move;
    }

    private Point getClosestTarget(AgentContext ctx) {
        double minDist = Double.POSITIVE_INFINITY;
        Point bestTarget = null;
        for (Point ourTarget : ourTargets) {
            double dist = ourTarget.distance(ctx.getX(), ctx.getY());
            if (dist < minDist) {
                bestTarget = ourTarget;
                minDist = dist;
            }
        }
        return bestTarget;
    }

    private void readMessages(AgentContext ctx) {
        for (Message message : ctx.getMessages()) {
            Point agent = readAgentPosition(message);
            if (agent != null) {
                markVisited(agent);
                if (pointList.contains(agent)) {
                    pointList.remove(agent);
                    if (agent.equals(target) && !ourTargets.contains(target)) {
                        target = null;
                    }
                }
            }
            Point target = readTargetPosition(message);
            if (target != null && !ourTargets.contains(target))
                ourTargets.add(target);
        }
    }

    private void sendTargetPositions(AgentContext ctx) {
        for (Entity entity : ctx.getNearbyTargets()) {
            if (entity.getID() != getId() && !foundTargets.contains(entity)) {
                foundTargets.add(entity);
                sendTargetPositon(ctx, entity);
            }
        }
    }

    private void sendPosition(AgentContext ctx) {
        final String message = "A" + getId() + ":" + ctx.getX() + ":" + ctx.getY();
        ctx.sendMessage(message, -1);
    }

    private void sendTargetPositon(AgentContext ctx, Entity target) {
        final String message = "T" + getId() + ":" + target.getX() + ":" + target.getY();
        ctx.sendMessage(message, target.getID());
    }

    private Point readTargetPosition(Message message) {
        String msg = message.getMessage();
        if (msg.startsWith("T")) {
            return getEntity(msg.substring(1));
        }
        return null;
    }

    private Point readAgentPosition(Message message) {
        String msg = message.getMessage();
        if (msg.startsWith("A")) {
            return getEntity(msg.substring(1));
        }
        return null;
    }

    private Point getEntity(String msg) {
        try {
            String[] info = msg.substring(1).split(":");
            int x = Integer.parseInt(info[1]);
            int y = Integer.parseInt(info[2]);
            return new Point(x, y);
        } catch (NumberFormatException nfe) {
            System.err.println("Bad message format!");
        }
        return null;
    }
}
