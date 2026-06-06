export default function PlaceholderPage({ title }) {
  return (
    <section className="pageStack">
      <div className="pageHeader">
        <p className="eyebrow">Next</p>
        <div>
          <h1>{title}</h1>
          <p>성능 개선 후 구현 예정</p>
        </div>
      </div>
    </section>
  );
}
