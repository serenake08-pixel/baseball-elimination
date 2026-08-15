import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import edu.princeton.cs.algs4.FlowEdge;
import edu.princeton.cs.algs4.FlowNetwork;
import edu.princeton.cs.algs4.FordFulkerson;
import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.StdOut;

public class BaseballElimination {
    private class Team {
        private int number;
        private int wins;
        private int losses;
        private int remaining;
        private int[] gamesLeft;

        private Team(int w, int l, int r, int[] rr, int num) {
            wins = w;
            losses = l;
            remaining = r;
            gamesLeft = rr;
            number = num;
        }

        public String toString(){
            return "idk name but yeah oops " + wins + losses + remaining + number;
        }
    }

    private int numTeams;
    private HashMap<String, Team> teams;
    // the bottom ones are buns and i should fix them soo
    private FordFulkerson[] ffs;
    private String[] newTeamIndexes;
    private ArrayList<String> teamNames;

    public BaseballElimination(String filename) {
        In in = new In(filename);
        teams = new HashMap<String, Team>();
        numTeams = in.readInt();
        ffs = new FordFulkerson[numTeams];
        teamNames = new ArrayList<String>();

        int count = 0;
        for (int k = 0; k < numTeams; k++){
            String n = in.readString();
            int w = in.readInt();
            int l = in.readInt();
            int r = in.readInt();
            int[] g = new int[numTeams];
            for (int i = 0; i < numTeams; i++) {
                g[i] = in.readInt();
            }
            teams.put(n, new Team(w, l, r, g, count));
            teamNames.add(n);
            count++;
        }
    }

    public int numberOfTeams() {
        return numTeams;
    }

    public Iterable<String> teams() {
        return teamNames;
    }

    public int wins(String team) {
        if (!teams.containsKey(team)){throw new IllegalArgumentException("aw hell no that's fake");}
        return teams.get(team).wins;
    }

    public int losses(String team) {
        if (!teams.containsKey(team)){throw new IllegalArgumentException("aw hell no that's fake");}
        return teams.get(team).losses;
    }

    public int remaining(String team) {
        if (!teams.containsKey(team)){throw new IllegalArgumentException("aw hell no that's fake");}
        return teams.get(team).remaining;
    }

    public int against(String team1, String team2) {
        if (!teams.containsKey(team1)){throw new IllegalArgumentException("aw hell no that's fake");}
        if (!teams.containsKey(team2)){throw new IllegalArgumentException("aw hell no that's fake");}
        return teams.get(team1).gamesLeft[teams.get(team2).number];
    }

    public boolean isEliminated(String team) {
        if (!teams.containsKey(team)){throw new IllegalArgumentException("aw hell no that's fake");}
        FlowNetwork network = new FlowNetwork(2 + (numTeams - 1) + (numTeams - 1) * (numTeams - 2) / 2);
        int s = 0;
        int t = network.V() - 1;

        // we reassign teams to numbers, excl the team of interest
        Iterator<String> teamsIterator = teams().iterator();
        newTeamIndexes = new String[numTeams-1];
        boolean f = false;
        for (int i = 0; i < numTeams; i++) {
            String name = teamsIterator.next();
            if (name.equals(team)) {
                f = true;
                continue;
            }
            if (f) {newTeamIndexes[i-1] = name;}
            else{newTeamIndexes[i] = name;}
        }

        int SCapacitiesSum = 0;

        for (int a = 0; a < numTeams - 1; a++) {
            int allowedWinA = teams.get(team).wins + teams.get(team).remaining - teams.get(newTeamIndexes[a]).wins;
            if (allowedWinA < 0){ //translation: team already cooked even if they win all those games
                ffs[teams.get(team).number] = null;
                return true;
            }
            network.addEdge(new FlowEdge(toVertex(a), t, allowedWinA));

            for (int b = a + 1; b < numTeams - 1; b++) {
                int capacity = against(newTeamIndexes[a], newTeamIndexes[b]);
                FlowEdge e = new FlowEdge(0, toVertex(a, b), capacity);
                SCapacitiesSum += capacity;
                network.addEdge(e);

                network.addEdge(new FlowEdge(toVertex(a, b), toVertex(a), Integer.MAX_VALUE));
                network.addEdge(new FlowEdge(toVertex(a, b), toVertex(b), Integer.MAX_VALUE));
            }
        }

        FordFulkerson ff = new FordFulkerson(network, s, t);
        ffs[teams.get(team).number] = ff;

        return ff.value() != SCapacitiesSum;
    }

    // the vertex number for the matchup teams
    private int toVertex(int a, int b) {
        // numTeams-2 + ... + numTeams-a-1 has a terms. Sum is...
        return (numTeams - 2 + numTeams - a - 1) * a / 2 + (b - a);
    }

    // the vertex number for the team's games won out of the remaining
    /*
     * do not mix up with newTeamIndexes[a]
     * that is for inputting the vertex reference and ouputting the String name.
     * this is for inputting the vertex reference and outputting the actual vertex
     * for the graph.
     */
    // bro this lowk confusing but don't worry about it
    private int toVertex(int a) {
        return (numTeams - 1) * (numTeams - 2) / 2 + a + 1;
    }

    // subset R of teams that eliminates given team; null if not eliminated
    // game node that source couldn't push through (part of mincut) --> edge to sink
    // is full -> team is problem
    public Iterable<String> certificateOfElimination(String team) {
        if (!isEliminated(team)) {
            return null;
        }

        FordFulkerson ff = ffs[teams.get(team).number];
        ArrayList<String> yuh = new ArrayList<String>();

        if (ff == null){ //trivial case
            for (String s : teams()){
                if (teams.get(s).wins > teams.get(team).wins + teams.get(team).remaining){
                    yuh.add(s);
                }
            }
            return yuh;
        }


        for (int a = 0; a < numTeams - 1; a++) {
            if (ff.inCut(toVertex(a))) {
                yuh.add(newTeamIndexes[a]);
            }
        }
        return yuh;
    }

    public static void main(String[] args) {
        BaseballElimination division = new BaseballElimination(args[0]);
        for (String team : division.teams()) {
            if (division.isEliminated(team)) {
                StdOut.print(team + " is eliminated by the subset R = { ");
                for (String t : division.certificateOfElimination(team)) {
                    StdOut.print(t + " ");
                }
                StdOut.println("}");
            } else {
                StdOut.println(team + " is not eliminated");
            }
        }
    }
}
