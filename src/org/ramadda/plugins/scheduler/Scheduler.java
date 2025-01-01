/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.scheduler;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;


import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.regex.*;


/**
 * Provides a top-level API
 *
 */
public class Scheduler extends RepositoryManager implements RequestHandler {

    /** _more_ */
    public static final String ARG_WEEKS = "weeks";

    /** _more_ */
    public static final String ARG_NUMPLAYERS = "numplayers";

    /** _more_ */
    public static final String ARG_PLAYERSPERGAME = "playerspergame";

    /** _more_ */
    public static final String ARG_NAMES = "names";

    /** _more_ */
    public static final String ARG_SEED = "seed";

    /** _more_ */
    public static final String ARG_MAXWITHOUT = "maxwithout";

    /** _more_ */
    public static final String ARG_ASCSV = "ascsv";

    /**
     *
     * @param repository _more_
     *
     * @throws Exception _more_
     */
    public Scheduler(Repository repository) throws Exception {
        super(repository);
    }




    /**
     * handle the request
     *
     * @param request request
     *
     * @return result
     *
     * @throws Exception on badness
     */
    public Result processScheduleRequest(Request request) throws Exception {
        boolean      asCsv = request.get(ARG_ASCSV, false);
        StringBuffer sb    = new StringBuffer();
        String       base  = getRepository().getUrlBase();
        if ( !request.defined(ARG_SEED)) {
            sb.append(msgHeader("Scheduler"));
            sb.append(HtmlUtils.form(base + "/scheduler/schedule"));
            sb.append(HtmlUtils.formTable());
            sb.append(HtmlUtils.formEntry(msgLabel("# Weeks"),
                                          HtmlUtils.input(ARG_WEEKS,
                                              request.getString(ARG_WEEKS,
                                                  "12"), HtmlUtils.SIZE_6)));
            sb.append(
                HtmlUtils.formEntry(
                    msgLabel("# Players"),
                    HtmlUtils.input(
                        ARG_NUMPLAYERS,
                        request.getString(ARG_NUMPLAYERS, "8"),
                        HtmlUtils.SIZE_6)));
            sb.append(HtmlUtils.formEntry(msgLabel("Or enter names"),
                                          HtmlUtils.textArea(ARG_NAMES,
                                              request.getString(ARG_NAMES,
                                                  ""), 8, 40)));
            sb.append(
                HtmlUtils.formEntry(
                    msgLabel("Players per week"),
                    HtmlUtils.input(
                        ARG_PLAYERSPERGAME,
                        request.getString(ARG_PLAYERSPERGAME, "4"),
                        HtmlUtils.SIZE_6)));

            sb.append(
                HtmlUtils.formEntry(
                    "", HtmlUtils.submit("Generate Schedule")));
            sb.append(HtmlUtils.formTableClose());
            sb.append(HtmlUtils.formClose());
        }


        if (request.defined(ARG_WEEKS)) {
            List<Player> players = new ArrayList<Player>();
            if (request.defined(ARG_NAMES)) {
                for (String line :
                        StringUtil.split(request.getString(ARG_NAMES, ""),
                                         "\n", true, true)) {
                    players.add(new Player(line));
                }
            } else {
                int numPlayers = request.get(ARG_NUMPLAYERS, 8);
                for (int i = 0; i < numPlayers; i++) {
                    players.add(new Player("Player " + (i + 1)));
                }
            }

            Random random;
            long   seed;
            String url;
            if (request.defined(ARG_SEED)) {
                random = new Random(seed = (long) request.get(ARG_SEED, 0));
            } else {
                seed = (long) (Math.random() * 100000);
                request.put(ARG_SEED, "" + seed);
                random = new Random(seed);
                sb.append(HtmlUtils.href(request.getUrl(),
                                         "Link to these results"));
            }
            schedule(sb, players, request.get(ARG_WEEKS, 0),
                     request.get(ARG_PLAYERSPERGAME, 4), random, asCsv);
        }

        return new Result("Scheduler", sb);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param players _more_
     * @param weeks _more_
     * @param playersPerGame _more_
     * @param random _more_
     * @param asCsv _more_
     */
    public void schedule(StringBuffer sb, List<Player> players, int weeks,
                         int playersPerGame, Random random, boolean asCsv) {
        if ( !asCsv) {
            sb.append("<pre>");
        }
        for (int week = 0; week < weeks; week++) {
            if ( !asCsv) {
                sb.append("Week " + (week + 1));
                sb.append("\n");
            }
            schedule(week, players, playersPerGame, random);
            for (Player player : players) {
                if (player.playing) {
                    for (Player otherPlayer : players) {
                        if ( !otherPlayer.equals(player)) {
                            otherPlayer.yourPlayingWith(player);
                            player.yourPlayingWith(otherPlayer);
                        }
                    }
                    if ( !asCsv) {
                        sb.append("\t" + player);
                        sb.append("\n");
                    } else {}
                }
            }
        }

        if ( !asCsv) {
            for (Player player : players) {
                if (player.playedWith.size() != players.size() - 1) {
                    sb.append("Player:" + player
                              + " not played with all players:"
                              + player.playedWith);
                    sb.append("\n");
                }
            }
            sb.append("</pre>");
            sb.append("\nSummary\n");
            sb.append(
                "<table cellpadding=4 cellspacing=0 border=1><tr><td>&nbsp;</td><td align=center colspan="
                + weeks + "><b>Weeks</b></td></tr>");
            sb.append("<tr><td>&nbsp;</td>");
            for (int i = 0; i < weeks; i++) {
                sb.append("<td>");
                if (i < 10) {
                    sb.append("&nbsp;");
                }
                sb.append("" + (i + 1));
                sb.append("</td>");
            }

            for (Player player : players) {
                sb.append("<tr><td>");
                sb.append(player.toString());
                sb.append("</td>");
                for (int i = 0; i < weeks; i++) {
                    if (player.isPlaying(i)) {
                        sb.append("<td bgcolor=#888>&nbsp;</td>");
                    } else {
                        sb.append("<td>&nbsp;</td>");
                    }
                }
                //                sb.append(StringUtil.join(" ", player.games));
                //                sb.append("</td></tr>");
                sb.append("</tr>");
            }
            sb.append("</table>");
        }

    }




    /**
     * _more_
     *
     * @param week _more_
     * @param players _more_
     * @param playersPerGame _more_
     * @param random _more_
     */
    public void schedule(int week, List<Player> players, int playersPerGame,
                         Random random) {
        List<Player> myPlayers              = new ArrayList<Player>(players);
        int          maxNumberOfGamesPlayed = 0;
        for (Player player : myPlayers) {
            player.playing = false;
            maxNumberOfGamesPlayed = Math.max(player.numGamesPlayed,
                    maxNumberOfGamesPlayed);
        }
        int selectedCnt = 0;
        //        System.err.println("week:" + week);
        for (Player player : players) {
            int weeksSincePlayed = week - player.lastPlayed;
            if (weeksSincePlayed > 2) {
                //                System.err.println("  adding:" + player +" " + player.lastPlayed);
                player.setPlaying(week, true);
                myPlayers.remove(player);
                if (selectedCnt++ >= playersPerGame) {
                    break;
                }
            }
        }
        if (selectedCnt >= playersPerGame) {
            return;
        }

        List<Player> playersToPickFrom = new ArrayList<Player>();
        for (Player player : myPlayers) {
            int weeksSincePlayed = week - player.lastPlayed;
            playersToPickFrom.add(player);
            if (weeksSincePlayed == 1) {
                continue;
            }
            playersToPickFrom.add(player);
            if (weeksSincePlayed == 2) {
                continue;
            }
            playersToPickFrom.add(player);
        }

        for (Player player : myPlayers) {
            int diff = maxNumberOfGamesPlayed - player.numGamesPlayed;
            while (diff > 0) {
                //System.err.println("weighting " + player + " max=" +maxNumberOfGamesPlayed +"   played:" +player.numGamesPlayed);
                playersToPickFrom.add(player);
                playersToPickFrom.add(player);
                diff--;
            }
        }


        //        System.err.println("pick:" + playersToPickFrom);
        while (selectedCnt < playersPerGame) {
            int    item   = random.nextInt(playersToPickFrom.size());
            Player player = playersToPickFrom.get(item);
            while (playersToPickFrom.remove(player)) {}
            player.setPlaying(week, true);
            selectedCnt++;
        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Aug 23, '13
     * @author         Enter your name here...
     */
    public static class Player {

        /** _more_ */
        String name;

        /** _more_ */
        boolean playing = false;

        /** _more_ */
        int lastPlayed = -1;

        /** _more_ */
        HashSet<Player> playedWith = new HashSet<Player>();

        /** _more_ */
        int numGamesPlayed = 0;

        /** _more_ */
        List<Integer> games = new ArrayList<Integer>();

        /** _more_ */
        HashSet<Integer> gamesPlayed = new HashSet<Integer>();

        /**
         * _more_
         *
         * @param name _more_
         */
        public Player(String name) {
            this.name = name;
        }


        /**
         * _more_
         *
         * @param week _more_
         * @param playing _more_
         */
        public void setPlaying(int week, boolean playing) {
            lastPlayed   = week;
            this.playing = playing;
            numGamesPlayed++;
            games.add(week + 1);
            gamesPlayed.add(week + 1);
        }

        /**
         * _more_
         *
         * @param week _more_
         *
         * @return _more_
         */
        public boolean isPlaying(int week) {
            return gamesPlayed.contains(week);
        }

        /**
         * _more_
         *
         * @param player _more_
         */
        public void yourPlayingWith(Player player) {
            playedWith.add(player);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int hashCode() {
            return name.hashCode();
        }


        /**
         * _more_
         *
         * @param o _more_
         *
         * @return _more_
         */
        public boolean equals(Object o) {
            if ( !(o instanceof Player)) {
                return false;
            }
            Player that = (Player) o;

            return that.name.equals(this.name);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            return name;
        }

    }



}
