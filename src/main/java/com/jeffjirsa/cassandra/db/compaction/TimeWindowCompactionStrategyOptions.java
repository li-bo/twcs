/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jeffjirsa.cassandra.db.compaction;

import com.google.common.collect.ImmutableList;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.db.compaction.*;

public final class TimeWindowCompactionStrategyOptions
{
    protected static final TimeUnit DEFAULT_TIMESTAMP_RESOLUTION = TimeUnit.MICROSECONDS;
    protected static final TimeUnit DEFAULT_COMPACTION_WINDOW_UNIT = TimeUnit.DAYS;
    protected static final int DEFAULT_COMPACTION_WINDOW_SIZE = 1;
    protected static final int DEFAULT_EXPIRED_SSTABLE_CHECK_FREQUENCY_SECONDS = 60 * 10;
    protected static final boolean DEFAULT_ENABLE_CLEANUP = false;

    protected static final String TIMESTAMP_RESOLUTION_KEY = "timestamp_resolution";
    protected static final String COMPACTION_WINDOW_UNIT_KEY = "compaction_window_unit";
    protected static final String COMPACTION_WINDOW_SIZE_KEY = "compaction_window_size";
    protected static final String EXPIRED_SSTABLE_CHECK_FREQUENCY_SECONDS_KEY = "expired_sstable_check_frequency_seconds";
    protected static final String PERFORM_CLEANUP = "enable_cleanup";

    protected final int sstableWindowSize;
    protected final TimeUnit sstableWindowUnit;
    protected final TimeUnit timestampResolution;
    protected final long expiredSSTableCheckFrequency;
    protected final boolean enableCleanup;

    SizeTieredCompactionStrategyOptions stcsOptions;

    protected final static ImmutableList<TimeUnit> validTimestampTimeUnits = ImmutableList.of(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, TimeUnit.MICROSECONDS, TimeUnit.NANOSECONDS);
    protected final static ImmutableList<TimeUnit> validWindowTimeUnits = ImmutableList.of(TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS);

    public TimeWindowCompactionStrategyOptions(Map<String, String> options)
    {
        String optionValue = options.get(TIMESTAMP_RESOLUTION_KEY);
        timestampResolution = optionValue == null ? DEFAULT_TIMESTAMP_RESOLUTION : TimeUnit.valueOf(optionValue);

        optionValue = options.get(COMPACTION_WINDOW_UNIT_KEY);
        sstableWindowUnit = optionValue == null ? DEFAULT_COMPACTION_WINDOW_UNIT : TimeUnit.valueOf(optionValue);

        optionValue = options.get(COMPACTION_WINDOW_SIZE_KEY);
        sstableWindowSize = optionValue == null ? DEFAULT_COMPACTION_WINDOW_SIZE : Integer.parseInt(optionValue);

        optionValue = options.get(EXPIRED_SSTABLE_CHECK_FREQUENCY_SECONDS_KEY);
        expiredSSTableCheckFrequency = TimeUnit.MILLISECONDS.convert(optionValue == null ? DEFAULT_EXPIRED_SSTABLE_CHECK_FREQUENCY_SECONDS : Long.parseLong(optionValue), TimeUnit.SECONDS);

        optionValue = options.get(PERFORM_CLEANUP);
        enableCleanup = Boolean.valueOf(optionValue);

        stcsOptions = new SizeTieredCompactionStrategyOptions(options);
    }

    public TimeWindowCompactionStrategyOptions()
    {
        sstableWindowUnit = DEFAULT_COMPACTION_WINDOW_UNIT;
        timestampResolution = DEFAULT_TIMESTAMP_RESOLUTION;
        sstableWindowSize = DEFAULT_COMPACTION_WINDOW_SIZE;
        expiredSSTableCheckFrequency = TimeUnit.MILLISECONDS.convert(DEFAULT_EXPIRED_SSTABLE_CHECK_FREQUENCY_SECONDS, TimeUnit.SECONDS);
        enableCleanup = DEFAULT_ENABLE_CLEANUP;

        stcsOptions = new SizeTieredCompactionStrategyOptions();
    }

    public static Map<String, String> validateOptions(Map<String, String> options, Map<String, String> uncheckedOptions) throws  ConfigurationException
    {
        String optionValue = options.get(TIMESTAMP_RESOLUTION_KEY);
        try
        {
            if (optionValue != null)
                if (!validTimestampTimeUnits.contains(TimeUnit.valueOf(optionValue)))
                    throw new ConfigurationException(String.format("%s is not valid for %s", optionValue, TIMESTAMP_RESOLUTION_KEY));
        }
        catch (IllegalArgumentException e)
        {
            throw new ConfigurationException(String.format("%s is not valid for %s", optionValue, TIMESTAMP_RESOLUTION_KEY));
        }


        optionValue = options.get(COMPACTION_WINDOW_UNIT_KEY);
        try
        {
            if (optionValue != null)
                if (!validWindowTimeUnits.contains(TimeUnit.valueOf(optionValue)))
                    throw new ConfigurationException(String.format("%s is not valid for %s", optionValue, COMPACTION_WINDOW_UNIT_KEY));

        }
        catch (IllegalArgumentException e)
        {
            throw new ConfigurationException(String.format("%s is not valid for %s", optionValue, COMPACTION_WINDOW_UNIT_KEY), e);
        }

        optionValue = options.get(COMPACTION_WINDOW_SIZE_KEY);
        try
        {
            int sstableWindowSize = optionValue == null ? DEFAULT_COMPACTION_WINDOW_SIZE : Integer.parseInt(optionValue);
            if (sstableWindowSize < 1)
            {
                throw new ConfigurationException(String.format("%s must be greater than 1", DEFAULT_COMPACTION_WINDOW_SIZE, sstableWindowSize));
            }
        }
        catch (NumberFormatException e)
        {
            throw new ConfigurationException(String.format("%s is not a parsable int (base10) for %s", optionValue, DEFAULT_COMPACTION_WINDOW_SIZE), e);
        }

        optionValue = options.get(EXPIRED_SSTABLE_CHECK_FREQUENCY_SECONDS_KEY);
        try
        {
            long expiredCheckFrequency = optionValue == null ? DEFAULT_EXPIRED_SSTABLE_CHECK_FREQUENCY_SECONDS : Long.parseLong(optionValue);
            if (expiredCheckFrequency < 0)
            {
                throw new ConfigurationException(String.format("%s must not be negative, but was %d", EXPIRED_SSTABLE_CHECK_FREQUENCY_SECONDS_KEY, expiredCheckFrequency));
             }
        }
        catch (NumberFormatException e)
        {
            throw new ConfigurationException(String.format("%s is not a parsable int (base10) for %s", optionValue, EXPIRED_SSTABLE_CHECK_FREQUENCY_SECONDS_KEY), e);
        }

        uncheckedOptions.remove(COMPACTION_WINDOW_SIZE_KEY);
        uncheckedOptions.remove(COMPACTION_WINDOW_UNIT_KEY);
        uncheckedOptions.remove(TIMESTAMP_RESOLUTION_KEY);
        uncheckedOptions.remove(EXPIRED_SSTABLE_CHECK_FREQUENCY_SECONDS_KEY);
        uncheckedOptions.remove(PERFORM_CLEANUP);

        uncheckedOptions = SizeTieredCompactionStrategyOptions.validateOptions(options, uncheckedOptions);

        return uncheckedOptions;
    }
}
