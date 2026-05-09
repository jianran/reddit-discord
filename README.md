# Reddit-Discord Economic Sentiment Bot

Spring Boot application that monitors Reddit for China/America economic discussions, analyzes sentiment with DeepSeek AI via Spring AI, and sends formatted DM reports to a Discord bot.

## Architecture

```
Reddit JSON API ──→ RedditClient ──→ RedditService
                                          │
                                    AnalysisOrchestrator ──→ DiscordClient ──→ Discord DM
                                          │
DeepSeek API ←── DeepSeekAnalysisService (Spring AI)
```

## Pipeline

1. **Reddit Search** — Searches top 10 China/America related subreddits and top 10 posters by engagement
2. **AI Analysis** — Sends posts to DeepSeek via Spring AI for summarization and economic scoring
3. **Discord DM** — Formats results with score bars, trending indicators, and sends via bot DM

## Prerequisites

- Java 17+
- Maven 3.8+
- [DeepSeek API key](https://platform.deepseek.com/)
- [Discord Bot token](https://discord.com/developers/applications)

## Setup

```bash
# Copy and fill in your keys
cp .env.example .env

# Build
./mvnw clean package -DskipTests

# Run
DEEPSEEK_API_KEY=sk-xxx \
DISCORD_BOT_TOKEN=xxx \
DISCORD_DM_USER_ID=123456789 \
  java -jar target/reddit-discord-0.0.1-SNAPSHOT.jar
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/analysis/run` | Run full pipeline, return analysis JSON |
| POST | `/api/analysis/run-and-dm` | Run pipeline and send Discord DM |

## Scheduled Execution

Runs automatically at 9 AM and 9 PM (Asia/Shanghai). Configurable via `app.analysis.cron` in `application.yml`.

## Configuration

Key settings in `src/main/resources/application.yml`:

| Property | Description | Default |
|----------|-------------|---------|
| `reddit.search-keywords` | Keywords for Reddit search | china, america, us-china, trade-war, tariffs |
| `reddit.subreddits` | Subreddits to search | worldnews, geopolitics, economics, news, china |
| `app.top-channels-count` | Number of top channels | 10 |
| `app.top-posters-count` | Number of top posters | 10 |
| `app.analysis.cron` | Schedule cron expression | `0 0 9,21 * * *` |
| `discord.enabled` | Enable Discord notifications | true |
