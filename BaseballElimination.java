import edu.princeton.cs.algs4.FlowEdge;
import edu.princeton.cs.algs4.FlowNetwork;
import edu.princeton.cs.algs4.FordFulkerson;
import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.Queue;
import edu.princeton.cs.algs4.SET;
import edu.princeton.cs.algs4.StdOut;

import java.util.HashMap;

public class BaseballElimination {
    private final HashMap<String, ScheduleItem> scheduleMap; // storing input here
    private FordFulkerson myFord; // maxflow class

    // create a baseball division from given filename in format specified below
    /* file input format:
4
Atlanta       83 71  8  0 1 6 1
Philadelphia  80 79  3  1 0 0 2
New_York      78 78  6  6 0 0 0
Montreal      77 82  3  1 2 0 0
     */

    public BaseballElimination(String filename) {
        In in = new In(filename);
        String[] stringArray = in.readAllLines();
        scheduleMap = new HashMap<>();
 
        // teams number
        int n = Integer.parseInt(stringArray[0]);
        for (int i = 1; i < stringArray.length; i++) {


            String[] string = extractStrings(stringArray[i], n);
            scheduleMap.put(string[0], new ScheduleItem(string, i - 1));
        }
    }


    // helper method for data extraction from input array
    private String[] extractStrings(String string, int teamNum) {
        String[] tempString = string.split(" ");
        String[] finalString = new String[teamNum + 4];
        int i = 0;
        for (String st : tempString) {
            if (!st.equals("")) finalString[i++] = st;
        }
        return finalString;
    }


    // nested class for Schedule item of input file (value item)
    private class ScheduleItem {
        // team number, number of wins, number of losses, number of remaning games
        private final int i, w, l, r;
        private final int[] g; // array of remaning games against each team

        // Shedule item construction
        public ScheduleItem(String[] string, int teamNum) {
            this.i = teamNum;
            this.w = Integer.parseInt(string[1]);
            this.l = Integer.parseInt(string[2]);
            this.r = Integer.parseInt(string[3]);
            g = new int[string.length - 4];
            for (int j = 4; j < string.length; j++) {

                this.g[j - 4] = Integer.parseInt(string[j]);
            }
        }
    }

    // number of teams
    public int numberOfTeams() {
        return scheduleMap.size();
    }

    // all teams
    public Iterable<String> teams() {
        return scheduleMap.keySet();
    }

    // Does given team participates at this division?
    private boolean checkTeam(String thisTeam) {
        for (String thatTeam : teams()) {
            if (thatTeam.equals(thisTeam)) return true;
        }
        return false;
    }

    // number of wins for given team
    public int wins(String team) {
        if (!checkTeam(team)) throw new IllegalArgumentException("Not appropriate team!");
        return scheduleMap.get(team).w;
    }

    // number of losses for given team
    public int losses(String team) {
        if (!checkTeam(team)) throw new IllegalArgumentException("Not appropriate team!");
        return scheduleMap.get(team).l;
    }

    // number of remaining games for given team
    public int remaining(String team) {
        if (!checkTeam(team)) throw new IllegalArgumentException("Not appropriate team!");
        return scheduleMap.get(team).r;
    }

    // number of remaining games between team1 and team2
    public int against(String team1, String team2) {
        if (!checkTeam(team1) || !checkTeam(team2))
            throw new IllegalArgumentException("Not appropriate team!");
        int[] gamesLeft = scheduleMap.get(team1).g; // game array for team1
        int i = scheduleMap.get(team2).i;  // i of team2
        return gamesLeft[i];
    }


    // nested class for maintainig a capacitated network for checking of given team
    private class MyMaxFlow {
        private final HashMap<Integer, String> currentMap;
        private final FlowNetwork flowNetwork;
        private int capSum; // capacity sum start-game edges
        private final SET<String> trivialEliminates;
        // teams name which trivialy eliminate this team

        public MyMaxFlow(String thisTeam) {
            flowNetwork = createFlowNetwork();
            currentMap = new HashMap<>();
            trivialEliminates = new SET<>();
            int firstKey = flowNetwork.V() - numberOfTeams(); // place of team at flownetwork
            for (String thatTeam : scheduleMap.keySet()) {
                if (!thatTeam.equals(thisTeam)) currentMap.put(firstKey++, thatTeam);
            }
            addEdges(thisTeam);
        }

        // return teams vertices numbers
        private Iterable<Integer> getTeamsVertices() {
            SET<Integer> vertSet = new SET<>();
            int firstKey = flowNetwork.V() - numberOfTeams();
            for (int key = firstKey; key < firstKey + numberOfTeams(); key++) {
                vertSet.add(key);
            }
            return vertSet;
        }


        // add game to network
        private void addEdges(String thisTeam) {
            Queue<String> queueTeams = new Queue<>();
            capSum = 0;
            for (String name : currentMap.values()) queueTeams.enqueue(name);

            int currentVert = 1; // vertice number of game
            while (!queueTeams.isEmpty()) {
                String firstTeam = queueTeams.dequeue();
                for (String secondTeam : queueTeams) {
                    FlowEdge sourseGameEdge = new FlowEdge(0, currentVert,
                                                           against(firstTeam, secondTeam));
                    capSum += against(firstTeam, secondTeam);
                    FlowEdge gameTeamEdge_1 = new FlowEdge(currentVert, getByVal(firstTeam),
                                                           Double.POSITIVE_INFINITY);
                    FlowEdge gameTeamEdge_2 = new FlowEdge(currentVert, getByVal(secondTeam),
                                                           Double.POSITIVE_INFINITY);
                    currentVert++;
                    flowNetwork.addEdge(sourseGameEdge);
                    flowNetwork.addEdge(gameTeamEdge_1);
                    flowNetwork.addEdge(gameTeamEdge_2);
                }
                int cap1 = wins(thisTeam) + remaining(thisTeam) - wins(firstTeam);
                // Trivial elimination
                if (cap1 < 0) {
                    cap1 = 0;
                    trivialEliminates.add(firstTeam);
                }
                FlowEdge teamFinishEdge_1 = new FlowEdge(getByVal(firstTeam),
                                                         flowNetwork.V() - 1, cap1);
                flowNetwork.addEdge(teamFinishEdge_1);
            }
        }

        // get key by value from currentMap
        private int getByVal(String team) {
            int returnKey = -1;
            for (int key : currentMap.keySet()) {
                if (currentMap.get(key).equals(team)) {
                    returnKey = key;
                    return returnKey;
                }
            }
            return returnKey;
        }

        // create flow network (team to be eliminated, scheduleMap)
        private FlowNetwork createFlowNetwork() {
            int n = numberOfTeams() - 1; // teams number
            int v = (int) (0.5 * n * (n - 1) + n + 2);
            FlowNetwork flowNetwork = new FlowNetwork(v);
            return flowNetwork;
        }

    }


    // is given team eliminated?
    public boolean isEliminated(String team) {
        if (!checkTeam(team)) throw new IllegalArgumentException("Not appropriate team!");
        MyMaxFlow myMaxFlow = new MyMaxFlow(team);
        if (!myMaxFlow.trivialEliminates.isEmpty()) return true;
        myFord = new FordFulkerson(myMaxFlow.flowNetwork, 0, myMaxFlow.flowNetwork.V() - 1);
        if (myFord.value() < myMaxFlow.capSum) return true;
        return false;
    }

    // subset R of teams that eliminates given team; null if not eliminated
    public Iterable<String> certificateOfElimination(String team) {
        if (!checkTeam(team)) throw new IllegalArgumentException("Not appropriate team!");
        MyMaxFlow myMaxFlow = new MyMaxFlow(team);
        if (!myMaxFlow.trivialEliminates.isEmpty()) return myMaxFlow.trivialEliminates;
        myFord = new FordFulkerson(myMaxFlow.flowNetwork, 0, myMaxFlow.flowNetwork.V() - 1);
        SET<String> cert = new SET<>();
        for (int v : myMaxFlow.getTeamsVertices()) {
            if (myFord.inCut(v)) {
                String teamCert = myMaxFlow.currentMap.get(v);
                cert.add(teamCert);
            }
        }
        if (cert.isEmpty()) cert = null;
        return cert;
    }


    // Test client: Have been team Hufflepuff elliminated?
    public static void main(String[] args) {
        BaseballElimination be = new BaseballElimination("teams4b.txt");
        String teamName = "Hufflepuff";
        StdOut.println(be.isEliminated(teamName));
        for (String st : be.certificateOfElimination(teamName)) StdOut.println(st);
    }
}
