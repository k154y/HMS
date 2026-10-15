-- Optional explicit price per selling package. NULL continues to use product base-unit price times factor.
ALTER TABLE commerce_product_units
  ADD COLUMN retail_package_price NUMERIC(19,4) CHECK (retail_package_price >= 0),
  ADD COLUMN wholesale_package_price NUMERIC(19,4) CHECK (wholesale_package_price >= 0);
