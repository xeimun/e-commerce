import {
  BadgeAlert,
  Construction,
  Lightbulb,
  Megaphone,
  NotebookPen,
  Shapes,
  ShieldCheck,
  Wrench
} from 'lucide-react';

const commonFacts = {
  environment: 'Windows, Spring Boot, PostgreSQL Docker Compose',
  tool: 'k6',
  comparison: '같은 데이터와 부하 조건에서 개선 전후 비교'
};

const report = {
  tabLabel: '성능개선 1',
  headline: '재고 차감 임계 구역을 줄여 주문 생성 처리량 34.8% 개선',
  problem:
    '인기 상품 주문이 같은 stocks 행에 몰리면 row-level lock 대기로 주문 생성 API 응답 시간이 길어질 수 있음',
  improvement:
    'critical section 최소화 관점에서 조건부 UPDATE를 적용해 재고 검증과 차감을 한 문장으로 처리함',
  resultNote:
    '처리량과 p95는 개선됐지만 p99 개선폭은 작아 꼬리 지연 요인은 남아 있을 수 있음',
  hypothesis:
    '기준 측정에서 주문은 모두 성공했고 최종 재고도 기대값과 일치했습니다. 다만 p95, p99 응답 시간이 높게 나타났습니다. 모든 요청이 같은 상품의 stocks 행을 차감하는 조건이었기 때문에 row-level lock 대기가 응답 지연에 영향을 줬을 것으로 가정했습니다.',
  method:
    '기존 방식은 stocks 행을 비관적 락으로 조회한 뒤 재고를 검증하고 차감했습니다. 개선 후에는 quantity >= 주문 수량 조건을 가진 UPDATE 한 문장으로 재고 검증과 차감을 처리했습니다. 락을 없애는 것이 아니라, 재고 차감이 필요한 구간을 짧게 만들기 위한 선택이었습니다.',
  limitation:
    'DB 락 대기 시간과 커넥션 풀 대기 시간을 별도 지표로 수집하지 못했습니다. 따라서 병목 원인은 응답 시간 변화와 코드 흐름을 근거로 한 가설로 남아 있습니다.'
};

const throughputMetric = {
  label: '주문 생성 처리량',
  unit: '건/초',
  before: 76.97,
  after: 103.76,
  direction: 'higher',
  domain: [70, 110]
};

const latencyMetrics = [
  {
    label: '평균 응답 시간',
    shortLabel: '평균',
    unit: 'ms',
    before: 1179.77,
    after: 870.26,
    direction: 'lower'
  },
  {
    label: 'p95 응답 시간',
    shortLabel: 'p95',
    unit: 'ms',
    before: 1610.89,
    after: 1342.18,
    direction: 'lower'
  },
  {
    label: 'p99 응답 시간',
    shortLabel: 'p99',
    unit: 'ms',
    before: 1630.56,
    after: 1450.14,
    direction: 'lower'
  }
];

const latencyDomain = [800, 1700];

const experimentConditionGroups = [
  {
    title: '부하 대상',
    items: [
      { label: '대상 상품', value: '상품 10001' },
      { label: '부하 방식', value: 'k6 shared-iterations, VU 100' }
    ]
  },
  {
    title: '주문 데이터',
    items: [
      { label: '장바구니', value: '고객별 1개' },
      { label: '주문 수량', value: '1개' },
      { label: '쿠폰', value: '없음' }
    ]
  },
  {
    title: '비교 시나리오',
    items: [
      { label: '충분한 재고', value: '초기 재고 1000개, 주문 생성 500건' },
      { label: '재고 소진', value: '초기 재고 100개, 주문 생성 200건' }
    ]
  }
];

const sufficientStockRows = [
  { metric: '성공 요청', before: '500건', after: '500건', change: '유지' },
  { metric: '실패 요청', before: '0건', after: '0건', change: '유지' },
  { metric: '주문 생성 구간', before: '6.50초', after: '4.82초', change: '25.8% 감소' },
  { metric: '처리량', before: '76.97건/초', after: '103.76건/초', change: '34.8% 증가' },
  { metric: '평균 응답 시간', before: '1,179.77ms', after: '870.26ms', change: '26.2% 감소' },
  { metric: 'p95 응답 시간', before: '1,610.89ms', after: '1,342.18ms', change: '16.7% 감소' },
  { metric: 'p99 응답 시간', before: '1,630.56ms', after: '1,450.14ms', change: '11.1% 감소' },
  { metric: '최종 재고', before: '500', after: '500', change: '유지' }
];

const stockOutRows = [
  { metric: '성공 요청', before: '100건', after: '100건', change: '유지' },
  { metric: '실패 요청', before: '100건', after: '100건', change: '유지' },
  { metric: '주문 생성 구간', before: '1.91초', after: '1.19초', change: '37.7% 감소' },
  { metric: '처리량', before: '104.93건/초', after: '168.49건/초', change: '60.6% 증가' },
  { metric: '평균 응답 시간', before: '865.31ms', after: '515.47ms', change: '40.4% 감소' },
  { metric: 'p95 응답 시간', before: '1,470.79ms', after: '858.91ms', change: '41.6% 감소' },
  { metric: 'p99 응답 시간', before: '1,498.78ms', after: '918.53ms', change: '38.7% 감소' },
  { metric: '최종 재고', before: '0', after: '0', change: '유지' }
];

const consistencyText =
  '성능 개선 후에도 주문과 재고 정합성이 깨지지 않는지 함께 확인했습니다. 충분한 재고 조건에서는 주문 500건이 모두 성공하고 최종 재고가 500으로 남았고, 재고 소진 조건에서는 성공 100건과 실패 100건, 최종 재고 0을 확인했습니다. 두 조건 모두 초과 판매는 0건이었습니다.';

function formatMetric(value, unit = '') {
  return `${value.toLocaleString('ko-KR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}${unit}`;
}

function getChangePercent(metric) {
  const change = ((metric.after - metric.before) / metric.before) * 100;
  return metric.direction === 'lower' ? Math.abs(change) : change;
}

function getScaledPercent(value, domain) {
  const [min, max] = domain;
  const percent = ((value - min) / (max - min)) * 100;
  return Math.min(100, Math.max(0, percent));
}

function ThroughputHighlightChart({ metric }) {
  const changePercent = getChangePercent(metric);
  const beforeHeight = getScaledPercent(metric.before, metric.domain);
  const afterHeight = getScaledPercent(metric.after, metric.domain);

  return (
    <div className="throughputHighlight">
      <div className="throughputSummary">
        <div>
          <span>{metric.label}</span>
          <strong>{changePercent.toFixed(1)}% 증가</strong>
        </div>
      </div>
      <div className="throughputColumnChart" aria-label={`${metric.label} 개선 전후 비교`}>
        <div className="throughputColumnGroup">
          <div className="throughputColumnTrack" aria-hidden="true">
            <div className="throughputColumn before" style={{ '--bar-size': `${beforeHeight}%` }} />
          </div>
          <span>개선 전</span>
          <strong>{formatMetric(metric.before, metric.unit)}</strong>
        </div>
        <div className="throughputColumnGroup">
          <div className="throughputColumnTrack" aria-hidden="true">
            <div className="throughputColumn after" style={{ '--bar-size': `${afterHeight}%` }} />
          </div>
          <span>개선 후</span>
          <strong>{formatMetric(metric.after, metric.unit)}</strong>
        </div>
      </div>
      <div className="chartAxisNote">
        축 범위 {metric.domain[0]}-{metric.domain[1]}{metric.unit}
      </div>
    </div>
  );
}

function LatencyComparison({ metrics }) {
  return (
    <div className="latencyComparePanel">
      <div className="chartPanelHeader">
        <div>
          <h3>주문 생성 API 응답 시간</h3>
        </div>
      </div>
      <div className="latencyCompareRows">
        {metrics.map((metric) => (
          <article className="latencyCompareItem" key={metric.label}>
            <div className="latencyCompareTitle">
              <strong>{metric.shortLabel}</strong>
              <span>{getChangePercent(metric).toFixed(1)}% 감소</span>
            </div>
            <div className="scaledRows">
              <div className="scaledRow compact">
                <span>전</span>
                <div className="scaledTrack" aria-hidden="true">
                  <div
                    className="scaledFill before"
                    style={{ '--bar-size': `${getScaledPercent(metric.before, latencyDomain)}%` }}
                  />
                </div>
                <strong>{formatMetric(metric.before, metric.unit)}</strong>
              </div>
              <div className="scaledRow compact">
                <span>후</span>
                <div className="scaledTrack" aria-hidden="true">
                  <div
                    className="scaledFill after blue"
                    style={{ '--bar-size': `${getScaledPercent(metric.after, latencyDomain)}%` }}
                  />
                </div>
                <strong>{formatMetric(metric.after, metric.unit)}</strong>
              </div>
            </div>
          </article>
        ))}
      </div>
      <div className="chartAxisNote">
        축 범위 {latencyDomain[0]}-{latencyDomain[1]}ms
      </div>
    </div>
  );
}

function MeasurementTable({ rows, title }) {
  return (
    <section className="measurementTableBlock">
      <h3>{title}</h3>
      <div className="measurementTableWrap">
        <table className="measurementTable">
          <thead>
            <tr>
              <th scope="col">지표</th>
              <th scope="col">개선 전</th>
              <th scope="col">개선 후</th>
              <th scope="col">변화</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={`${title}-${row.metric}`}>
                <th scope="row">{row.metric}</th>
                <td>{row.before}</td>
                <td>{row.after}</td>
                <td>{row.change}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function NumberedSection({ children, icon: Icon, number, title }) {
  return (
    <section className="reportNarrativeBlock">
      <div className="reportNarrativeIndex">
        <span>{number}</span>
      </div>
      <div className="reportNarrativeBody">
        <div className="reportNarrativeTitle">
          <Icon aria-hidden="true" size={18} />
          <h2>{title}</h2>
        </div>
        {children}
      </div>
    </section>
  );
}

export default function ReportPage() {
  return (
    <section className="pageStack reportPage">
      <div className="pageHeader">
        <p className="eyebrow">Performance Report</p>
        <div>
          <h1>성능 리포트</h1>
        </div>
      </div>

      <section className="reportCommonPanel" aria-labelledby="common-panel-title">
        <div className="panelHeader">
          <Shapes aria-hidden="true" size={18} />
          <h2 id="common-panel-title">공통 측정 조건</h2>
        </div>
        <div className="commonConditionLayout">
          <section className="commonFactCard commonEnvironment" aria-labelledby="common-environment-title">
            <h3 id="common-environment-title">측정 환경</h3>
            <p>{commonFacts.environment}</p>
          </section>
          <section className="commonFactCard">
            <h3>측정 도구</h3>
            <p>{commonFacts.tool}</p>
          </section>
          <section className="commonFactCard">
            <h3>비교 방식</h3>
            <p>{commonFacts.comparison}</p>
          </section>
        </div>
      </section>

      <div className="reportTabs" role="tablist" aria-label="성능 개선 리포트">
        <button className="segmentedTab active" type="button" role="tab" aria-selected="true">
          {report.tabLabel}
        </button>
      </div>

      <article className="reportStory" aria-labelledby="report-story-title">
        <div className="reportStoryHero">
          <h2 id="report-story-title">{report.headline}</h2>
          <div className="reportStorySummary" aria-label="성능 개선 요약">
            <article>
              <BadgeAlert aria-hidden="true" size={18} />
              <div>
                <h3>문제</h3>
                <p>{report.problem}</p>
              </div>
            </article>
            <article>
              <Wrench aria-hidden="true" size={18} />
              <div>
                <h3>개선</h3>
                <p>{report.improvement}</p>
              </div>
            </article>
          </div>
        </div>

        <section className="reportChartSection" aria-labelledby="chart-section-title">
          <div className="sectionTitle">
            <div>
              <h2 id="chart-section-title">핵심 지표 비교</h2>
            </div>
          </div>

          <div className="reportChartLayout">
            <section className="reportFeaturedMetric" aria-label="주문 생성 처리량 비교">
              <div className="chartPanelHeader">
                <div>
                  <h3>대표 지표</h3>
                </div>
              </div>
              <ThroughputHighlightChart metric={throughputMetric} />
            </section>

            <LatencyComparison metrics={latencyMetrics} />
          </div>

          {report.resultNote && (
            <p className="chartReadingNote">
              <Megaphone aria-hidden="true" size={18} />
              {report.resultNote}
            </p>
          )}

          <details className="reportDetails experimentDetails">
            <summary>
              <span className="detailsToggleIcon" aria-hidden="true" />
              <span>측정 조건 보기</span>
            </summary>
            <div className="experimentDetailsBody">
              {experimentConditionGroups.map((group) => (
                <section className="experimentConditionGroup" key={group.title}>
                  <h3>{group.title}</h3>
                  <dl>
                    {group.items.map((item) => (
                      <div key={`${group.title}-${item.label}`}>
                        <dt>{item.label}</dt>
                        <dd>{item.value}</dd>
                      </div>
                    ))}
                  </dl>
                </section>
              ))}
            </div>
          </details>

          <details className="reportDetails measurementDetails">
            <summary>
              <span className="detailsToggleIcon" aria-hidden="true" />
              <span>상세 측정값 보기</span>
            </summary>
            <div className="measurementDetailsBody">
              <MeasurementTable rows={sufficientStockRows} title="충분한 재고 조건 측정값" />
              <MeasurementTable rows={stockOutRows} title="재고 소진 조건 측정값" />
            </div>
          </details>
        </section>

        <div className="reportNarrativeList">
          <NumberedSection icon={NotebookPen} number="1" title="병목 가설">
            <p>{report.hypothesis}</p>
          </NumberedSection>

          <NumberedSection icon={Lightbulb} number="2" title="개선 방법과 이유">
            <p>{report.method}</p>
          </NumberedSection>

          <NumberedSection icon={ShieldCheck} number="3" title="정합성 검증">
            <p>{consistencyText}</p>
          </NumberedSection>

          {report.limitation && (
            <NumberedSection icon={Construction} number="4" title="측정 한계">
              <p>{report.limitation}</p>
            </NumberedSection>
          )}
        </div>
      </article>
    </section>
  );
}
