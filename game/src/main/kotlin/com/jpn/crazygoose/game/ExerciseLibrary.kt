package com.jpn.crazygoose.game

import com.jpn.crazygoose.game.AgeGroup.AGE_10_13
import com.jpn.crazygoose.game.AgeGroup.AGE_3_5
import com.jpn.crazygoose.game.AgeGroup.AGE_6_9
import com.jpn.crazygoose.game.Category.ADRESSE
import com.jpn.crazygoose.game.Category.CARDIO
import com.jpn.crazygoose.game.Category.COORDINATION
import com.jpn.crazygoose.game.Category.EQUILIBRE
import com.jpn.crazygoose.game.Category.RENFORCEMENT
import com.jpn.crazygoose.game.Category.SOUPLESSE
import com.jpn.crazygoose.game.Equipment.BALLON
import com.jpn.crazygoose.game.Equipment.BANC
import com.jpn.crazygoose.game.Equipment.BATON
import com.jpn.crazygoose.game.Equipment.CERCEAU
import com.jpn.crazygoose.game.Equipment.CORDE
import com.jpn.crazygoose.game.Equipment.COUSSIN
import com.jpn.crazygoose.game.Equipment.CRAIE
import com.jpn.crazygoose.game.Equipment.ELASTIQUE
import com.jpn.crazygoose.game.Equipment.FRISBEE
import com.jpn.crazygoose.game.Equipment.PLOTS
import com.jpn.crazygoose.game.Equipment.RAQUETTES
import com.jpn.crazygoose.game.Equipment.SEAU
import com.jpn.crazygoose.game.Equipment.TRAMPOLINE

/**
 * Bibliothèque de base du prototype. Elle sera remplacée par la liste de 60 exercices validée par JPN.
 * Exclus d'office : contact physique entre joueurs et sauts depuis une hauteur.
 */
object ExerciseLibrary {
    val base: List<Exercise> = listOf(
        // Sans matériel
        Exercise("sauts-etoile", "Sauts en étoile", "Saute en écartant bras et jambes, puis reviens pieds joints.", CARDIO, baseReps = 20),
        Exercise("course-sur-place", "Course sur place", "Cours sur place en montant bien les genoux.", CARDIO),
        Exercise("squats", "Squats", "Pieds écartés, plie les genoux comme pour t'asseoir, puis remonte.", RENFORCEMENT, baseReps = 12),
        Exercise("grenouille", "Sauts de grenouille", "Accroupis-toi, mains au sol, puis saute vers l'avant.", CARDIO, baseReps = 8),
        Exercise("flamant", "Le flamant rose", "Tiens-toi sur un pied, bras écartés, sans poser l'autre pied.", EQUILIBRE),
        Exercise("pas-chasses", "Pas chassés", "Déplace-toi sur le côté en pas chassés, aller et retour.", COORDINATION),
        Exercise("ours", "La marche de l'ours", "Marche à quatre pattes, genoux décollés du sol.", RENFORCEMENT, minAge = AGE_3_5),
        Exercise("crabe", "La marche du crabe", "Assis, mains derrière toi, soulève les fesses et avance.", RENFORCEMENT, minAge = AGE_6_9),
        Exercise("gainage", "La planche", "En appui sur les avant-bras et les pieds, garde le corps bien droit.", RENFORCEMENT, minAge = AGE_6_9),
        Exercise("pompes", "Pompes", "Mains au sol, descends la poitrine puis pousse. Genoux au sol si besoin.", RENFORCEMENT, minAge = AGE_10_13, baseReps = 8),
        Exercise("fentes", "Fentes", "Fais un grand pas en avant et plie les deux genoux, puis change de jambe.", RENFORCEMENT, minAge = AGE_6_9, baseReps = 10),
        Exercise("toucher-pieds", "Touche tes pieds", "Jambes tendues, descends doucement les mains vers tes pieds.", SOUPLESSE),
        Exercise("moulins", "Moulins à vent", "Fais de grands cercles avec les bras, vers l'avant puis vers l'arrière.", SOUPLESSE),
        Exercise("statue", "La statue", "Cours sur place, puis fige-toi à chaque fois que tu comptes jusqu'à 5.", COORDINATION),
        Exercise("talons-fesses", "Talons-fesses", "Cours sur place en touchant tes fesses avec tes talons.", CARDIO),
        Exercise("sauts-cote", "Sauts sur le côté", "Pieds joints, saute à droite puis à gauche par-dessus une ligne imaginaire.", CARDIO, baseReps = 16),
        Exercise("chaise", "La chaise", "Dos contre un mur ou un arbre, cuisses à l'horizontale, tiens la position.", RENFORCEMENT, minAge = AGE_10_13),
        Exercise("avion", "L'avion", "Sur un pied, penche-toi en avant, bras écartés comme des ailes.", EQUILIBRE, minAge = AGE_6_9),
        Exercise("danse", "Danse libre", "Danse comme tu veux, sans t'arrêter !", CARDIO),
        Exercise("tour-jardin", "Le tour du jardin", "Fais un tour en courant et reviens à ta place.", CARDIO),
        // Avec matériel
        Exercise("dribbles", "Dribbles", "Fais rebondir le ballon sans le perdre.", ADRESSE, equipment = BALLON, minAge = AGE_6_9),
        Exercise("tirs-cible", "Tirs vers une cible", "Vise un arbre ou un plot avec le ballon, à 5 pas.", ADRESSE, equipment = BALLON, baseReps = 6),
        Exercise("trampoline", "Sauts sur le trampoline", "Saute en comptant tes sauts, bien au centre.", CARDIO, equipment = TRAMPOLINE, baseReps = 20),
        Exercise("corde", "Corde à sauter", "Saute à la corde, pieds joints.", CARDIO, equipment = CORDE, minAge = AGE_6_9, baseReps = 20),
        Exercise("baton-equilibre", "Bâton en équilibre", "Tiens le bâton debout sur ta main sans qu'il tombe.", ADRESSE, equipment = BATON),
        Exercise("seau-balles", "Lancers dans le seau", "Lance des balles ou des pommes de pin dans le seau à 3 pas.", ADRESSE, equipment = SEAU, baseReps = 8),
        Exercise("slalom", "Slalom", "Fais un slalom entre les plots, aller et retour.", CARDIO, equipment = PLOTS),
        Exercise("hula-hoop", "Hula-hoop", "Fais tourner le cerceau autour de ta taille.", COORDINATION, equipment = CERCEAU, minAge = AGE_6_9),
        Exercise("frisbee", "Lancers de frisbee", "Lance le frisbee le plus loin possible puis va le chercher.", ADRESSE, equipment = FRISBEE, baseReps = 4),
        Exercise("elastique", "Tirage élastique", "Tends l'élastique devant toi en écartant les bras.", RENFORCEMENT, equipment = ELASTIQUE, minAge = AgeGroup.AGE_14_17, baseReps = 12),
        Exercise("montee-marche", "Montées de marche", "Monte et descends d'une marche ou d'un banc stable.", CARDIO, equipment = BANC, baseReps = 16),
        Exercise("marelle", "Marelle", "Dessine une marelle à la craie et parcours-la.", COORDINATION, equipment = CRAIE),
        Exercise("jongles-raquette", "Jongles à la raquette", "Fais rebondir la balle sur ta raquette.", ADRESSE, equipment = RAQUETTES, minAge = AGE_6_9, baseReps = 10),
        Exercise("coussin-tete", "Le coussin sur la tête", "Marche avec le coussin sur la tête sans le faire tomber.", EQUILIBRE, equipment = COUSSIN),
    )
}
