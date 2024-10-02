package com.mojang.realmsclient.util;

import java.util.Set;

public record WorldGenerationInfo(String seed, LevelType levelType, boolean generateStructures, Set<String> experiments)
{
}
